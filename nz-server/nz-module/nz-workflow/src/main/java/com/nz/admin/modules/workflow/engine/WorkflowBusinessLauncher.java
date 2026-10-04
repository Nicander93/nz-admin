package com.nz.admin.modules.workflow.engine;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.module.NzWorkflowBusinessHandler;
import com.nz.admin.common.module.NzWorkflowBusinessLauncher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/** 只由业务模块调用；业务键锁保证不同幂等键也不会重复启动在途审批。 */
@Service
@ConditionalOnProperty(name = "warm-flow.enabled", havingValue = "true")
public class WorkflowBusinessLauncher implements NzWorkflowBusinessLauncher {
    private final WarmFlowRuntime runtime;
    private final WorkflowBusinessEvents events;
    private final JdbcTemplate jdbc;
    private final ObjectProvider<NzWorkflowBusinessHandler> handlers;
    public WorkflowBusinessLauncher(WarmFlowRuntime runtime, WorkflowBusinessEvents events, JdbcTemplate jdbc,
                                    ObjectProvider<NzWorkflowBusinessHandler> handlers) {
        this.runtime = runtime;
        this.events = events;
        this.jdbc = jdbc;
        this.handlers = handlers;
    }
    @Override
    public java.util.List<NzWorkflowBusinessLauncher.Definition> available(String type) {
        var user = runtime.identity();
        return jdbc.query("SELECT flow_code,flow_name FROM flow_definition WHERE tenant_id=? AND del_flag='0' AND is_publish=1 AND business_type=? ORDER BY flow_name LIMIT 200",
                (rs, row) -> new NzWorkflowBusinessLauncher.Definition(rs.getString("flow_code"), rs.getString("flow_name")), user.getTenantId().toString(), type);
    }
    @Override
    @Transactional
    public String start(String type, String businessId, String code, Map<String, Object> variables) {
        var user = runtime.identity();
        String tenant = user.getTenantId().toString();
        if (handlers.stream().filter(h -> type.equals(h.businessType())).count() != 1)
            throw new BusinessException("业务流程处理器尚未正确装配");
        jdbc.update("INSERT INTO nz_workflow_business (tenant_id,business_type,business_id,event_sequence,flow_status) VALUES (?,?,?,0,'0') ON CONFLICT (tenant_id,business_type,business_id) DO NOTHING", tenant, type, businessId);
        var binding = jdbc.queryForMap("SELECT instance_id,flow_status FROM nz_workflow_business WHERE tenant_id=? AND business_type=? AND business_id=? FOR UPDATE", tenant, type, businessId);
        if (binding.get("instance_id") != null && Set.of("0", "1", "13").contains(binding.get("flow_status").toString()))
            return binding.get("instance_id").toString();
        runtime.lockDefinitionCode(code, user);
        if (jdbc.queryForList("SELECT id FROM flow_definition WHERE tenant_id=? AND flow_code=? AND business_type=? AND is_publish=1 AND del_flag='0'", tenant, code, type).isEmpty())
            throw new BusinessException("请选择已发布且属于该业务用途的流程");
        Long instance = runtime.start(code, businessId, variables);
        jdbc.update("UPDATE nz_workflow_business SET instance_id=?,event_sequence=0 WHERE tenant_id=? AND business_type=? AND business_id=?", instance, tenant, type, businessId);
        events.record(instance, tenant, user.getUserId().toString(), "START");
        return instance.toString();
    }
}
