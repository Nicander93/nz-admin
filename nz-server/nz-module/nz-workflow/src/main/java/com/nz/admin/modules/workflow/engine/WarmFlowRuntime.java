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

    public WarmFlowRuntime(LoginUserContext users) {
        this.users = users;
    }

    private LoginUser identity() {
        var user = users.getLoginUserOrNull();
        if (user == null
                || user.getTenantId() == null
                || !Objects.equals(user.getTenantId(), TenantContextHolder.getTenantIdOrNull()))
            throw new BusinessException("缺少可信登录租户身份");
        return user;
    }

    private void checkTenant(String tenant, LoginUser user) {
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
        identity();
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
        return FlowEngine.defService().importDef(definition).getId();
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

    private EngineDefinitionRequest model(DefJson definition) {
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

    @Transactional
    public void publish(Long id) {
        var user = identity();
        var definition = FlowEngine.defService().getById(id);
        if (definition == null) throw new BusinessException("流程定义不存在");
        checkTenant(definition.getTenantId(), user);
        if (!FlowEngine.defService().publish(id)) throw new BusinessException("流程发布失败");
    }

    @Transactional
    public Long start(String code, String businessId, Map<String, Object> variables) {
        var user = identity();
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
                        instance.getFlowName(),
                        instance.getBusinessId(),
                        instance.getFlowStatus()),
                tasks.stream()
                        .map(task -> new TaskView(task.getId().toString(), task.getNodeName()))
                        .toList(),
                history.stream()
                        .map(
                                task ->
                                        new HistoryView(
                                                task.getNodeName(),
                                                task.getApprover(),
                                                task.getMessage(),
                                                task.getSkipType()))
                        .toList());
    }

    @Transactional
    public Long action(Long id, String type, String comment, Map<String, Object> variables) {
        var user = identity();
        var task = FlowEngine.taskService().getById(id);
        if (task == null) throw new BusinessException("待办任务不存在或已办理");
        checkTenant(task.getTenantId(), user);
        if (!FlowEngine.userService().getPermission(id).contains(user.getUserId().toString()))
            throw new BusinessException("不是该任务的办理人");
        if (!Set.of("PASS", "REJECT").contains(type)) throw new BusinessException("不支持的办理类型");
        return FlowEngine.taskService()
                .skip(
                        id,
                        parameters(user)
                                .skipType(type)
                                .message(comment)
                                .variable(variables == null ? Map.of() : variables))
                .getId();
    }

    @Transactional
    public void deleteInstance(Long id) {
        var user = identity();
        var instance = FlowEngine.insService().getById(id);
        if (instance == null) throw new BusinessException("流程实例不存在");
        checkTenant(instance.getTenantId(), user);
        if (!Objects.equals(instance.getCreateBy(), user.getUserId().toString())
                || !FlowEngine.taskService().getByInsId(id).isEmpty())
            throw new BusinessException("只有发起人可删除已结束的实例");
        FlowEngine.insService().remove(List.of(id));
    }

    @Transactional
    public void deleteDefinition(Long id) {
        var user = identity();
        var definition = FlowEngine.defService().getById(id);
        if (definition == null) throw new BusinessException("流程定义不存在");
        checkTenant(definition.getTenantId(), user);
        if (!FlowEngine.insService().getByDefId(id).isEmpty())
            throw new BusinessException("定义存在引用实例，不能删除");
        FlowEngine.defService().removeDef(List.of(id));
    }

    public record InstanceView(String id, String flowName, String businessId, String flowStatus) {}

    public record TaskView(String id, String nodeName) {}

    public record HistoryView(String nodeName, String approver, String message, String skipType) {}

    public record Snapshot(
            InstanceView instance, List<TaskView> tasks, List<HistoryView> history) {}
}
