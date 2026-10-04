package com.nz.admin.modules.workflow.engine;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.common.core.BusinessException;
import jakarta.validation.Validator;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.DefJson;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 官方设计器协议适配；客户端只能修改允许的声明式字段和坐标。 */
@Service
@ConditionalOnProperty(name = "warm-flow.enabled", havingValue = "true")
public class WorkflowDesignerService {
    private final WarmFlowRuntime runtime;
    private final Validator validator;
    private final JdbcTemplate jdbc;
    private final WorkflowBusinessEvents events;
    public WorkflowDesignerService(WarmFlowRuntime runtime, Validator validator, JdbcTemplate jdbc, WorkflowBusinessEvents events) {
        this.runtime = runtime;
        this.validator = validator;
        this.jdbc = jdbc;
        this.events = events;
    }
    public List<Map<String, String>> businessTypes() { runtime.identity(); return events.types(); }

    public List<Map<String, Object>> definitions() {
        String tenant = runtime.identity().getTenantId().toString();
        return jdbc.query("SELECT id, flow_code, flow_name, version, is_publish, COALESCE(business_type,'') AS business_type FROM flow_definition WHERE del_flag='0' AND tenant_id=? ORDER BY update_time DESC, id DESC LIMIT 200",
                (rs, row) -> Map.of("id", rs.getString("id"), "flowCode", rs.getString("flow_code"),
                        "flowName", rs.getString("flow_name"), "version", rs.getString("version"),
                        "isPublish", rs.getInt("is_publish"), "businessType", rs.getString("business_type")), tenant);
    }
    public DefJson query(Long id) {
        runtime.definition(id);
        var result = FlowEngine.defService().queryDesign(id).setId(id);
        int index = 0;
        for (var node : result.getNodeList()) {
            if (StrUtil.isBlank(node.getCoordinate())) node.setCoordinate((200 + index * 220) + ",240");
            if (!node.getCoordinate().contains("|")) node.setCoordinate(node.getCoordinate() + "|" + node.getCoordinate());
            index++;
        }
        return result;
    }
    @Transactional
    public Long create(String code, String name, Long sourceId, String type) {
        if (sourceId != null) {
            var model = runtime.definition(sourceId);
            Long id = runtime.importDefinition(new EngineDefinitionRequest(model.flowCode(), model.flowName(), model.nodeList()));
            var source = query(sourceId);
            source.setId(id).setVersion(FlowEngine.defService().getById(id).getVersion()).setIsPublish(0);
            save(source);
            return id;
        }
        var nodes = List.of(new EngineDefinitionRequest.Node("start", "开始", 0, null, "0",
                        List.of(new EngineDefinitionRequest.Transition("review", "PASS", null))),
                new EngineDefinitionRequest.Node("review", "审批", 1, "user:" + runtime.identity().getUserId(), "0",
                        List.of(new EngineDefinitionRequest.Transition("end", "PASS", null))),
                new EngineDefinitionRequest.Node("end", "结束", 2, null, "0", List.of()));
        var request = new EngineDefinitionRequest(code, name, nodes);
        validate(request);
        if (StrUtil.isNotBlank(type) && events.types().stream().noneMatch(value -> type.equals(value.get("code")))) throw new BusinessException("业务用途未装配");
        var user = runtime.identity();
        runtime.lockDefinitionCode(code, user);
        var existing = jdbc.queryForList("SELECT COALESCE(business_type,'') AS business_type FROM flow_definition WHERE tenant_id=? AND flow_code=? AND del_flag='0' LIMIT 1", user.getTenantId().toString(), code);
        String selected = StrUtil.blankToDefault(type, "");
        if (!existing.isEmpty() && !Objects.equals(existing.get(0).get("business_type"), selected)) throw new BusinessException("同一流程编码不能更换业务用途，请创建新编码");
        Long id = runtime.importDefinition(request);
        jdbc.update("UPDATE flow_definition SET business_type=? WHERE id=? AND tenant_id=?", selected, id, user.getTenantId().toString());
        return id;
    }
    @Transactional
    public void save(DefJson input) {
        if (input.getId() == null) throw new BusinessException("请先创建流程草稿");
        var user = runtime.identity();
        var existing = FlowEngine.defService().getById(input.getId());
        if (existing == null) throw new BusinessException("流程不存在或无权访问");
        runtime.checkTenant(existing.getTenantId(), user);
        runtime.lockDefinitionCode(existing.getFlowCode(), user);
        if (!Objects.equals(existing.getFlowCode(), input.getFlowCode())) throw new BusinessException("流程编码不能在设计器中更改");
        // 与发布竞争时锁住同一行，防止发布后又被旧设计器覆盖。
        var ids = jdbc.queryForList("SELECT id FROM flow_definition WHERE id=? AND tenant_id=? FOR UPDATE",
                Long.class, input.getId(), user.getTenantId().toString());
        if (ids.isEmpty()) throw new BusinessException("流程不存在或无权访问");
        var stored = FlowEngine.defService().getById(input.getId());
        if (!Objects.equals(stored.getIsPublish(), 0) || !FlowEngine.insService().getByDefId(input.getId()).isEmpty())
            throw new BusinessException("已发布或使用的定义不能覆盖，请创建新版本");
        if (!"CLASSICS".equals(input.getModelValue())) throw new BusinessException("当前接入支持经典模型，请使用经典设计器");
        rejectExecutionFields(input.getListenerType(), input.getListenerPath(), input.getFormPath(), input.getExt());
        if (input.getNodeList() == null || input.getNodeList().size() > 100) throw new BusinessException("节点数量无效");
        for (var node : input.getNodeList()) {
            rejectExecutionFields(node.getListenerType(), node.getListenerPath(), node.getFormPath(), node.getExt());
            if (StrUtil.isNotBlank(node.getAnyNodeSkip()) && !"N".equals(node.getAnyNodeSkip()))
                throw new BusinessException("暂不支持任意节点跳转配置");
        }
        var request = runtime.model(input);
        validate(request);
        var clean = runtime.convertModel(request).setId(stored.getId()).setVersion(stored.getVersion()).setModelValue("CLASSICS");
        for (int i = 0; i < clean.getNodeList().size(); i++) {
            var original = input.getNodeList().get(i);
            var node = clean.getNodeList().get(i).setCoordinate(coordinate(original.getCoordinate()));
            for (int j = 0; j < node.getSkipList().size(); j++) {
                var edge = original.getSkipList().get(j);
                node.getSkipList().get(j).setCoordinate(coordinate(edge.getCoordinate()))
                        .setSkipName(StrUtil.subPre(edge.getSkipName(), 100));
            }
        }
        try {
            FlowEngine.defService().saveDef(clean, false);
        } catch (Exception error) {
            // 上游 saveDef 声明 checked Exception，转为业务异常以回滚整个保存事务。
            throw new BusinessException("流程模型保存失败: " + StrUtil.subPre(error.getMessage(), 150));
        }
    }
    private void validate(EngineDefinitionRequest model) {
        if (!validator.validate(model).isEmpty()) throw new BusinessException("模型字段格式无效，请检查名称、节点与办理人");
    }
    private void rejectExecutionFields(String type, String path, String form, String ext) {
        if (StrUtil.isNotBlank(type) || StrUtil.isNotBlank(path) || StrUtil.isNotBlank(form)
                || (StrUtil.isNotBlank(ext) && !List.of("[]", "{}", "null").contains(ext)))
            throw new BusinessException("监听器、动态表单路径与自定义扩展尚未开放，请通过业务接口接入");
    }
    private String coordinate(String value) {
        if (StrUtil.isBlank(value)) return null;
        if (value.length() > 1000 || !value.matches("[-0-9.,;| ]+")) throw new BusinessException("坐标格式无效");
        return value;
    }
}
