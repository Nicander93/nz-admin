package com.nz.admin.modules.workflow.engine;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.nz.admin.common.module.NzWorkflowBusinessHandler;
import org.dromara.warm.flow.core.FlowEngine;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/** 事件与引擎事务一同落库，单条回调失败不丢失；多节点通过数据库锁竞争投递。 */
@Service
@ConditionalOnProperty(name = "warm-flow.enabled", havingValue = "true")
public class WorkflowBusinessEvents {
    private final JdbcTemplate jdbc;
    private final ObjectProvider<NzWorkflowBusinessHandler> handlers;
    private final TransactionTemplate transactions;
    public WorkflowBusinessEvents(JdbcTemplate jdbc, ObjectProvider<NzWorkflowBusinessHandler> handlers,
                                  PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.handlers = handlers;
        this.transactions = new TransactionTemplate(manager);
        this.transactions.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    /** 调用方已经持有实例行锁；同一实例的序号在所有节点上递增。 */
    public void record(Long instanceId, String tenant, String actor, String operation) {
        var bindings = jdbc.queryForList("SELECT business_type, business_id, event_sequence FROM nz_workflow_business WHERE tenant_id=? AND instance_id=? FOR UPDATE", tenant, instanceId);
        if (bindings.isEmpty()) return;
        var binding = bindings.get(0);
        long sequence = ((Number) binding.get("event_sequence")).longValue() + 1;
        var instance = FlowEngine.insService().getById(instanceId);
        String status = instance.getFlowStatus();
        jdbc.update("UPDATE nz_workflow_business SET event_sequence=?, flow_status=? WHERE tenant_id=? AND instance_id=?", sequence, status, tenant, instanceId);
        jdbc.update("INSERT INTO nz_workflow_event (id,tenant_id,business_type,business_id,instance_id,event_sequence,flow_status,node_type,actor,operation,business_snapshot,attempts,next_attempt,delivered) VALUES (?,?,?,?,?,?,?,?,?,?,?,0,CURRENT_TIMESTAMP,FALSE)",
                IdUtil.getSnowflakeNextId(), tenant, binding.get("business_type"), binding.get("business_id"), instanceId,
                sequence, status, instance.getNodeType(), actor, operation, cn.hutool.json.JSONUtil.toJsonStr(handlers.orderedStream().filter(h -> binding.get("business_type").equals(h.businessType())).findFirst().map(h -> h.detail(tenant, binding.get("business_id").toString())).orElse(java.util.Map.of())));
    }
    public java.util.List<java.util.Map<String, String>> types() {
        return handlers.orderedStream().map(h -> java.util.Map.of("code", h.businessType(), "name", h.businessName())).toList();
    }

    public java.util.Map<String, Boolean> status(Long instance, String tenant) {
        var rows = jdbc.queryForList("SELECT attempts FROM nz_workflow_event WHERE tenant_id=? AND instance_id=? AND delivered=FALSE", tenant, instance);
        return java.util.Map.of("pending", !rows.isEmpty(), "failed", rows.stream().anyMatch(row -> ((Number) row.get("attempts")).intValue() > 0));
    }

    public java.util.Map<String, Object> detail(Long instance, String tenant) {
        var rows = jdbc.queryForList("SELECT business_snapshot FROM nz_workflow_event WHERE tenant_id=? AND instance_id=? ORDER BY event_sequence DESC LIMIT 1", tenant, instance);
        return rows.isEmpty() ? java.util.Map.of() : cn.hutool.json.JSONUtil.parseObj(rows.get(0).get("business_snapshot").toString());
    }

    @Scheduled(fixedDelayString = "${nz.workflow.callback-delay-ms:1000}")
    public void retry() {
        for (int i = 0; i < 20; i++) {
            Boolean delivered = transactions.execute(status -> deliverOne());
            if (!Boolean.TRUE.equals(delivered)) break;
        }
    }
    private boolean deliverOne() {
        var rows = jdbc.queryForList("SELECT e.* FROM nz_workflow_event e WHERE e.delivered=FALSE AND e.next_attempt<=CURRENT_TIMESTAMP AND NOT EXISTS (SELECT 1 FROM nz_workflow_event p WHERE p.tenant_id=e.tenant_id AND p.instance_id=e.instance_id AND p.event_sequence<e.event_sequence AND p.delivered=FALSE) ORDER BY e.next_attempt,e.id LIMIT 1 FOR UPDATE SKIP LOCKED");
        if (rows.isEmpty()) return false;
        var row = rows.get(0);
        var type = row.get("business_type").toString();
        var event = new NzWorkflowBusinessHandler.Event(row.get("tenant_id").toString(), row.get("business_id").toString(),
                row.get("instance_id").toString(), ((Number) row.get("event_sequence")).longValue(),
                row.get("flow_status").toString(), ((Number) row.get("node_type")).intValue(), row.get("actor").toString(), row.get("operation").toString());
        var matching = handlers.orderedStream().filter(h -> type.equals(h.businessType())).toList();
        try {
            if (matching.size() != 1) throw new IllegalStateException("业务处理器未装配或重复: " + type);
            // 回调使用独立事务，失败时撤销其业务修改，事件保留等待重试。
            transactions.executeWithoutResult(status -> matching.get(0).apply(event));
            jdbc.update("UPDATE nz_workflow_event SET delivered=TRUE,last_error=NULL WHERE id=?", row.get("id"));
        } catch (RuntimeException error) {
            int attempts = ((Number) row.get("attempts")).intValue() + 1;
            long delay = Math.min(300, 1L << Math.min(attempts, 8));
            jdbc.update("UPDATE nz_workflow_event SET attempts=?,last_error=?,next_attempt=? WHERE id=?", attempts,
                    StrUtil.subPre(error.getMessage(), 500), java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(delay)), row.get("id"));
        }
        return true;
    }
}
