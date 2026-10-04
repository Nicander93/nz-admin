package com.nz.admin.modules.workflow.engine;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.auth.core.LoginUser;
import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.tenant.core.TenantContextHolder;

import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.*;
import org.dromara.warm.flow.core.entity.*;
import org.dromara.warm.flow.core.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** 新实例走 Warm-Flow，旧实例继续走 legacy 服务；每次访问再次校验租户与参与人。 */
@Service
@ConditionalOnProperty(name = "warm-flow.enabled", havingValue = "true")
public class WarmFlowRuntime {
    private final LoginUserContext users;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final WorkflowBusinessEvents events;

    public WarmFlowRuntime(LoginUserContext users, org.springframework.jdbc.core.JdbcTemplate jdbc, WorkflowBusinessEvents events) {
        this.users = users;
        this.jdbc = jdbc;
        this.events = events;
    }

    LoginUser identity() {
        var user = users.getLoginUserOrNull();
        if (user == null
                || user.getTenantId() == null
                || !Objects.equals(user.getTenantId(), TenantContextHolder.getTenantIdOrNull()))
            throw new BusinessException("缺少可信登录租户身份");
        return user;
    }

    void checkTenant(String tenant, LoginUser user) {
        if (!Objects.equals(tenant, user.getTenantId().toString()))
            throw new BusinessException("流程不存在或无权访问");
    }

    private FlowParams parameters(LoginUser user) {
        return FlowParams.build()
                .handler(user.getUserId().toString())
                .permissionFlag(List.of(user.getUserId().toString()));
    }

    /** 导入生成新版本，忽略客户端 ID、租户和监听器；不会覆盖在途实例。 */
    @Transactional
    public Long importDefinition(EngineDefinitionRequest request) {
        var user = identity();
        var clean = convertModel(request);
        lockDefinitionCode(request.flowCode(), user);
        var previous = jdbc.queryForList("SELECT business_type FROM flow_definition WHERE tenant_id=? AND flow_code=? AND del_flag='0' ORDER BY id DESC LIMIT 1", user.getTenantId().toString(), request.flowCode());
        Long id = FlowEngine.defService().importDef(clean).getId();
        if (!previous.isEmpty() && previous.get(0).get("business_type") != null)
            jdbc.update("UPDATE flow_definition SET business_type=? WHERE id=? AND tenant_id=?", previous.get(0).get("business_type"), id, user.getTenantId().toString());
        return id;
    }

    DefJson convertModel(EngineDefinitionRequest request) {
        validateModel(request);
        Set<String> codes = new HashSet<>();
        for (var node : request.nodeList())
            if (!codes.add(node.nodeCode())) throw new BusinessException("节点编码重复");
        var definition =
                new DefJson()
                        .setFlowCode(request.flowCode())
                        .setFlowName(request.flowName())
                        .setIsPublish(0);
        var nodes = new ArrayList<NodeJson>();
        for (var node : request.nodeList()) {
            if (node.nodeType() == 1
                    && (node.permissionFlag() == null
                            || FlowEngine.permissionHandler()
                                    .convertPermissions(List.of(node.permissionFlag().split(",")))
                                    .isEmpty())) throw new BusinessException("审批节点必须指定当前租户中的有效办理人");
            var converted =
                    new NodeJson()
                            .setNodeCode(node.nodeCode())
                            .setNodeName(node.nodeName())
                            .setNodeType(node.nodeType())
                            .setPermissionFlag(
                                    node.permissionFlag() == null
                                            ? null
                                            : node.permissionFlag()
                                                    .replace(
                                                            ",",
                                                            org.dromara.warm.flow.core.constant
                                                                    .FlowCons.SPLIT_AT))
                            .setNodeRatio(node.nodeRatio() == null ? "0" : node.nodeRatio());
            var skips = new ArrayList<SkipJson>();
            for (var transition :
                    node.skipList() == null
                            ? List.<EngineDefinitionRequest.Transition>of()
                            : node.skipList()) {
                if (!codes.contains(transition.nextNodeCode()))
                    throw new BusinessException("目标节点不存在");
                String condition = transition.skipCondition();
                if (condition != null
                        && !condition.isBlank()
                        && !condition.matches(
                                "(eq|ne|ge|gt|le|lt|like|notLike)@@[A-Za-z][A-Za-z0-9_]*\\|[A-Za-z0-9_"
                                    + " .-]+"))
                    throw new BusinessException("仅支持内置比较条件，例如 ge@@amount|100");
                skips.add(
                        new SkipJson()
                                .setNowNodeCode(node.nodeCode())
                                .setNextNodeCode(transition.nextNodeCode())
                                .setSkipType(
                                        transition.skipType() == null
                                                ? "PASS"
                                                : transition.skipType())
                                .setSkipCondition(condition));
            }
            converted.setSkipList(skips);
            nodes.add(converted);
        }
        definition.setNodeList(nodes);
        return definition;
    }

    private void validateModel(EngineDefinitionRequest request) {
        var nodes = request.nodeList();
        if (nodes.stream().filter(n -> n.nodeType() == 0).count() != 1
                || nodes.stream().filter(n -> n.nodeType() == 2).count() != 1)
            throw new BusinessException("流程必须有唯一开始和结束节点");
        Map<String, Set<String>> forward = new HashMap<>(), reverse = new HashMap<>();
        for (var node : nodes) {
            if (node.nodeRatio() != null
                    && new java.math.BigDecimal(node.nodeRatio())
                                    .compareTo(new java.math.BigDecimal("100"))
                            > 0) throw new BusinessException("通过比例不能超过 100");
            forward.computeIfAbsent(node.nodeCode(), key -> new HashSet<>());
            for (var edge :
                    node.skipList() == null
                            ? List.<EngineDefinitionRequest.Transition>of()
                            : node.skipList()) {
                forward.get(node.nodeCode()).add(edge.nextNodeCode());
                reverse.computeIfAbsent(edge.nextNodeCode(), key -> new HashSet<>())
                        .add(node.nodeCode());
            }
        }
        String start =
                nodes.stream().filter(n -> n.nodeType() == 0).findFirst().orElseThrow().nodeCode();
        String end =
                nodes.stream().filter(n -> n.nodeType() == 2).findFirst().orElseThrow().nodeCode();
        if (reachable(start, forward).size() != nodes.size()
                || reachable(end, reverse).size() != nodes.size())
            throw new BusinessException("所有节点必须从开始可达，并能到达结束");
    }

    private Set<String> reachable(String start, Map<String, Set<String>> edges) {
        var visited = new HashSet<String>();
        var queue = new ArrayDeque<String>();
        queue.add(start);
        while (!queue.isEmpty()) {
            String node = queue.removeFirst();
            if (visited.add(node)) queue.addAll(edges.getOrDefault(node, Set.of()));
        }
        return visited;
    }

    /** 读取声明式模型用于继续编辑；保存时导入新版本。 */
    public EngineDefinitionRequest definition(Long id) {
        var user = identity();
        var definition = FlowEngine.defService().getById(id);
        if (definition == null) throw new BusinessException("流程定义不存在");
        checkTenant(definition.getTenantId(), user);
        return model(FlowEngine.defService().queryDesign(id));
    }

    EngineDefinitionRequest model(DefJson definition) {
        return new EngineDefinitionRequest(
                definition.getFlowCode(),
                definition.getFlowName(),
                definition.getNodeList().stream()
                        .map(
                                node ->
                                        new EngineDefinitionRequest.Node(
                                                node.getNodeCode(),
                                                node.getNodeName(),
                                                node.getNodeType(),
                                                node.getPermissionFlag() == null
                                                        ? null
                                                        : node.getPermissionFlag()
                                                                .replace(
                                                                        org.dromara.warm.flow.core
                                                                                .constant.FlowCons
                                                                                .SPLIT_AT,
                                                                        ","),
                                                node.getNodeRatio(),
                                                (node.getSkipList() == null
                                                                ? List.<SkipJson>of()
                                                                : node.getSkipList())
                                                        .stream()
                                                                .map(
                                                                        edge ->
                                                                                new EngineDefinitionRequest
                                                                                        .Transition(
                                                                                        edge
                                                                                                .getNextNodeCode(),
                                                                                        edge
                                                                                                .getSkipType(),
                                                                                        edge
                                                                                                .getSkipCondition()))
                                                                .toList()))
                        .toList());
    }

    /** PostgreSQL 事务锁按租户和流程编码串行化版本创建与发布，支持跨后端节点。 */
    void lockDefinitionCode(String code, LoginUser user) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtext(?),hashtext(?))", rs -> {}, user.getTenantId().toString(), code);
    }

    @Transactional
    public void publish(Long id) {
        var user = identity();
        var definition = FlowEngine.defService().getById(id);
        if (definition == null) throw new BusinessException("流程定义不存在");
        checkTenant(definition.getTenantId(), user);
        lockDefinitionCode(definition.getFlowCode(), user);
        jdbc.queryForList("SELECT id FROM flow_definition WHERE id=? AND tenant_id=? FOR UPDATE", Long.class, id, user.getTenantId().toString());
        definition = FlowEngine.defService().getById(id);
        if (Objects.equals(definition.getIsPublish(), 1)) return;
        if (!Objects.equals(definition.getIsPublish(), 0)) throw new BusinessException("历史版本不能重新发布，请创建新版本");
        convertModel(model(FlowEngine.defService().queryDesign(id)));
        if (!FlowEngine.defService().publish(id)) throw new BusinessException("流程发布失败");
    }

    @Transactional
    public Long start(String code, String businessId, Map<String, Object> variables) {
        var user = identity();
        lockDefinitionCode(code, user);
        var definition = FlowEngine.defService().getPublishByFlowCode(code);
        if (definition == null) throw new BusinessException("已发布的流程定义不存在");
        checkTenant(definition.getTenantId(), user);
        return FlowEngine.insService()
                .start(
                        businessId,
                        parameters(user)
                                .flowCode(code)
                                .variable(variables == null ? Map.of() : variables))
                .getId();
    }

    public List<Map<String, Object>> publishedDefinitions() {
        var user = identity();
        return jdbc.query("SELECT flow_code,flow_name FROM flow_definition WHERE del_flag='0' AND tenant_id=? AND is_publish=1 ORDER BY flow_name LIMIT 200",
                (rs, row) -> Map.of("flowCode", rs.getString("flow_code"), "flowName", rs.getString("flow_name")), user.getTenantId().toString());
    }

    /** 参与人可读取实例及其轨迹，菜单权限本身不授予对象访问权。 */
    public Snapshot get(Long id) {
        var user = identity();
        var instance = FlowEngine.insService().getById(id);
        if (instance == null) throw new BusinessException("流程实例不存在");
        checkTenant(instance.getTenantId(), user);
        var tasks = FlowEngine.taskService().getByInsId(id);
        var history = FlowEngine.hisTaskService().getByInsId(id);
        String actor = user.getUserId().toString();
        boolean participant =
                Objects.equals(instance.getCreateBy(), actor)
                        || tasks.stream()
                                .anyMatch(
                                        task ->
                                                FlowEngine.userService()
                                                        .getPermission(task.getId())
                                                        .contains(actor))
                        || history.stream()
                                .anyMatch(task -> Objects.equals(task.getApprover(), actor));
        if (!participant) throw new BusinessException("无权读取该流程实例");
        return new Snapshot(
                new InstanceView(
                        instance.getId().toString(),
                        instance.getFlowName() == null ? FlowEngine.defService().getById(instance.getDefinitionId()).getFlowName() : instance.getFlowName(),
                        instance.getBusinessId(),
                        instance.getFlowStatus(), Objects.equals(instance.getCreateBy(), actor), instance.getNodeType() != 2),
                tasks.stream()
                        .map(task -> new TaskView(task.getId().toString(), task.getNodeName(), FlowEngine.userService().getPermission(task.getId()).contains(actor)))
                        .toList(),
                history.stream()
                        .map(
                                task ->
                                        new HistoryView(
                                                task.getNodeName(),
                                                task.getApprover(),
                                                task.getMessage(),
                                                task.getSkipType()))
                        .toList(), events.detail(id, user.getTenantId().toString()), events.status(id, user.getTenantId().toString()));
    }

    @Transactional
    public Long action(Long id, String type, String comment, Map<String, Object> variables) {
        var user = identity();
        var task = ownedTask(id, user);
        if (!Set.of("PASS", "REJECT").contains(type)) throw new BusinessException("不支持的办理类型");
        var params = parameters(user).skipType(type).message(comment).variable(variables == null ? Map.of() : variables);
        boolean rejectRoute = FlowEngine.skipService().getByDefId(task.getDefinitionId()).stream()
                .anyMatch(edge -> task.getNodeCode().equals(edge.getNowNodeCode()) && "REJECT".equals(edge.getSkipType()));
        // 没有退回连线时将申请退回到业务草稿；引擎不允许跳转开始节点。
        var result = "REJECT".equals(type) && !rejectRoute
                ? FlowEngine.taskService().termination(id, params.flowStatus("9"))
                : FlowEngine.taskService().skip(id, params);
        events.record(result.getId(), user.getTenantId().toString(), user.getUserId().toString(), type);
        return result.getId();
    }

    @Transactional
    public Long revoke(Long id, String comment) {
        var user = identity();
        lockInstance(id, user);
        var instance = FlowEngine.insService().getById(id);
        if (!Objects.equals(instance.getCreateBy(), user.getUserId().toString())) throw new BusinessException("只有发起人可以撤回");
        if (FlowEngine.taskService().getByInsId(id).isEmpty()) throw new BusinessException("流程已结束，不能撤回");
        // 上游 revoke 会重新产生首个审批节点的任务；业务撤回采用原生结束操作并记录 CANCEL 状态。
        // ignore 仅在服务端核验申请人后设置，客户端无法授予此权限。
        FlowEngine.taskService().terminationByInsId(id, parameters(user).message(comment).flowStatus("6").ignore(true));
        events.record(id, user.getTenantId().toString(), user.getUserId().toString(), "REVOKE");
        return id;
    }

    public List<Map<String, String>> returnNodes(Long taskId) {
        var user = identity();
        var task = FlowEngine.taskService().getById(taskId);
        if (task == null) throw new BusinessException("待办已办理");
        checkTenant(task.getTenantId(), user);
        if (!FlowEngine.userService().getPermission(taskId).contains(user.getUserId().toString())) throw new BusinessException("不是该任务的办理人");
        var visited = FlowEngine.hisTaskService().getByInsId(task.getInstanceId()).stream().map(HisTask::getNodeCode).collect(java.util.stream.Collectors.toSet());
        return FlowEngine.nodeService().getByDefId(task.getDefinitionId()).stream()
                .filter(n -> (n.getNodeType() == 1) && visited.contains(n.getNodeCode()) && !n.getNodeCode().equals(task.getNodeCode()))
                .map(n -> Map.of("nodeCode", n.getNodeCode(), "nodeName", n.getNodeName())).toList();
    }

    @Transactional
    public Long manage(Long taskId, String operation, List<String> targets, String nodeCode, String comment) {
        var user = identity();
        var task = ownedTask(taskId, user);
        var params = parameters(user).message(comment);
        if (Set.of("TRANSFER", "DEPUTE", "ADD", "REDUCE").contains(operation)) {
            if (targets == null || targets.isEmpty() || targets.size() > 20
                    || targets.stream().anyMatch(v -> !v.matches("[1-9][0-9]*"))) throw new BusinessException("请选择有效办理人");
            var resolved = FlowEngine.permissionHandler().convertPermissions(targets);
            if (!new HashSet<>(resolved).equals(new HashSet<>(targets))) throw new BusinessException("办理人必须属于当前租户且处于启用状态");
            if (Set.of("TRANSFER", "DEPUTE").contains(operation) && (targets.size() != 1 || targets.contains(user.getUserId().toString())))
                throw new BusinessException("转办或委派需选择一名其他办理人");
            if ("REDUCE".equals(operation)) {
                var current = FlowEngine.userService().getPermission(taskId);
                if (!current.containsAll(targets) || new HashSet<>(targets).size() >= new HashSet<>(current).size()) throw new BusinessException("减签必须保留至少一名现有办理人");
            }
            params.addHandlers(targets).reductionHandlers(targets);
        }
        switch (operation) {
            case "TRANSFER" -> FlowEngine.taskService().transfer(taskId, params);
            case "DEPUTE" -> FlowEngine.taskService().depute(taskId, params);
            case "ADD" -> FlowEngine.taskService().addSignature(taskId, params.reductionHandlers(null));
            case "REDUCE" -> FlowEngine.taskService().reductionSignature(taskId, params.addHandlers(null));
            case "RETURN" -> {
                if (returnNodes(taskId).stream().noneMatch(n -> Objects.equals(n.get("nodeCode"), nodeCode))) throw new BusinessException("只能退回本实例已到达的审批节点");
                FlowEngine.taskService().skip(taskId, params.skipType("REJECT").nodeCode(nodeCode));
            }
            case "TERMINATE" -> FlowEngine.taskService().termination(taskId, params);
            default -> throw new BusinessException("不支持的操作");
        }
        events.record(task.getInstanceId(), user.getTenantId().toString(), user.getUserId().toString(), operation);
        return task.getInstanceId();
    }

    private Task ownedTask(Long id, LoginUser user) {
        var task = FlowEngine.taskService().getById(id);
        if (task == null) throw new BusinessException("待办任务不存在或已办理");
        checkTenant(task.getTenantId(), user);
        lockInstance(task.getInstanceId(), user);
        task = FlowEngine.taskService().getById(id);
        if (task == null || !FlowEngine.userService().getPermission(id).contains(user.getUserId().toString()))
            throw new BusinessException("不是该任务的办理人，或任务已经办理");
        return task;
    }

    private void lockInstance(Long id, LoginUser user) {
        if (jdbc.queryForList("SELECT id FROM flow_instance WHERE id=? AND tenant_id=? FOR UPDATE",
                Long.class, id, user.getTenantId().toString()).isEmpty()) throw new BusinessException("流程不存在或无权访问");
    }

    /** 列表始终限定当前租户及本人，不用前端传入用户标识。 */
    public List<Map<String, Object>> center(String category, int page, int size) {
        var user = identity();
        String predicate = switch (category) {
            case "applications" -> "i.create_by=?";
            case "pending" -> "EXISTS (SELECT 1 FROM flow_task t JOIN flow_user u ON u.associated=t.id AND u.tenant_id=t.tenant_id WHERE t.del_flag='0' AND u.del_flag='0' AND t.instance_id=i.id AND t.tenant_id=i.tenant_id AND u.processed_by=?)";
            case "completed" -> "EXISTS (SELECT 1 FROM flow_his_task h WHERE h.del_flag='0' AND h.instance_id=i.id AND h.tenant_id=i.tenant_id AND h.approver=?)";
            default -> throw new BusinessException("未知流程列表");
        };
        return jdbc.query("SELECT i.id,COALESCE(i.flow_name,d.flow_name,'审批流程') AS flow_name,i.business_id,i.flow_status,i.create_time FROM flow_instance i LEFT JOIN flow_definition d ON d.id=i.definition_id AND d.tenant_id=i.tenant_id WHERE i.del_flag='0' AND i.tenant_id=? AND " + predicate + " ORDER BY i.create_time DESC,i.id DESC LIMIT ? OFFSET ?",
                (rs, row) -> Map.of("id", rs.getString("id"), "flowName", rs.getString("flow_name"),
                        "businessId", rs.getString("business_id"), "flowStatus", rs.getString("flow_status"),
                        "createTime", rs.getTimestamp("create_time") == null ? "" : rs.getTimestamp("create_time").toLocalDateTime().toString()),
                user.getTenantId().toString(), user.getUserId().toString(), size, (page - 1) * size);
    }

    @Transactional
    public void deleteInstance(Long id) {
        var user = identity();
        lockInstance(id, user);
        var instance = FlowEngine.insService().getById(id);
        if (instance == null) throw new BusinessException("流程实例不存在");
        checkTenant(instance.getTenantId(), user);
        if (!Objects.equals(instance.getCreateBy(), user.getUserId().toString())
                || !FlowEngine.taskService().getByInsId(id).isEmpty())
            throw new BusinessException("只有发起人可删除已结束的实例");
        if (!jdbc.queryForList("SELECT instance_id FROM nz_workflow_event WHERE tenant_id=? AND instance_id=? LIMIT 1", user.getTenantId().toString(), id).isEmpty())
            throw new BusinessException("业务关联实例需保留审批轨迹，不能单独删除");
        FlowEngine.insService().remove(List.of(id));
    }

    @Transactional
    public void deleteDefinition(Long id) {
        var user = identity();
        var definition = FlowEngine.defService().getById(id);
        if (definition == null) throw new BusinessException("流程定义不存在");
        checkTenant(definition.getTenantId(), user);
        lockDefinitionCode(definition.getFlowCode(), user);
        if (!FlowEngine.insService().getByDefId(id).isEmpty())
            throw new BusinessException("定义存在引用实例，不能删除");
        FlowEngine.defService().removeDef(List.of(id));
    }

    public record InstanceView(String id, String flowName, String businessId, String flowStatus, boolean creator, boolean active) {}

    public record TaskView(String id, String nodeName, boolean actionable) {}

    public record HistoryView(String nodeName, String approver, String message, String skipType) {}

    public record Snapshot(
            InstanceView instance, List<TaskView> tasks, List<HistoryView> history, Map<String, Object> business, Map<String, Boolean> sync) {}
}
