package com.nz.admin.modules.workflow.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.common.module.NzUserNotification;
import com.nz.admin.common.module.NzUserNotificationPublisher;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.core.PageResult;
import com.nz.admin.framework.auth.core.LoginUser;
import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowDefinitionDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowTaskDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowInstanceDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowInstanceEventDO;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceActionRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceStartRequest;
import com.nz.admin.modules.workflow.entity.vo.WorkflowInstanceEventVO;
import com.nz.admin.modules.workflow.entity.vo.WorkflowInstanceVO;
import com.nz.admin.modules.workflow.mapper.WorkflowDefinitionMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowInstanceEventMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowInstanceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

/**
 * 流程实例服务实现。
 */
@Service
public class WorkflowInstanceServiceImpl extends ServiceImpl<WorkflowInstanceMapper, WorkflowInstanceDO>
        implements WorkflowInstanceService {

    private static final String RUNNING = "RUNNING";

    private final WorkflowDefinitionMapper definitionMapper;
    private final WorkflowInstanceEventMapper eventMapper;
    private final LoginUserContext loginUserContext;
    private final WorkflowRuntimeResolver runtimeResolver;
    private final WorkflowTaskLifecycleService taskLifecycleService;
    private final Optional<NzUserNotificationPublisher> notificationPublisher;

    public WorkflowInstanceServiceImpl(WorkflowDefinitionMapper definitionMapper,
                                       WorkflowInstanceEventMapper eventMapper,
                                       LoginUserContext loginUserContext,
                                       WorkflowRuntimeResolver runtimeResolver,
                                       WorkflowTaskLifecycleService taskLifecycleService,
                                       Optional<NzUserNotificationPublisher> notificationPublisher) {
        this.definitionMapper = definitionMapper;
        this.eventMapper = eventMapper;
        this.loginUserContext = loginUserContext;
        this.runtimeResolver = runtimeResolver;
        this.taskLifecycleService = taskLifecycleService;
        this.notificationPublisher = notificationPublisher;
    }

    @Override
    public PageResult<WorkflowInstanceVO> page(Integer pageNum, Integer pageSize, String flowCode, String title,
                                               String businessKey, String status, boolean mine) {
        LoginUser loginUser = requireLoginUser();
        LambdaQueryWrapper<WorkflowInstanceDO> wrapper = new LambdaQueryWrapper<WorkflowInstanceDO>()
                .like(StrUtil.isNotBlank(flowCode), WorkflowInstanceDO::getFlowCode, StrUtil.trim(flowCode))
                .like(StrUtil.isNotBlank(title), WorkflowInstanceDO::getTitle, StrUtil.trim(title))
                .like(StrUtil.isNotBlank(businessKey), WorkflowInstanceDO::getBusinessKey, StrUtil.trim(businessKey))
                .eq(StrUtil.isNotBlank(status), WorkflowInstanceDO::getStatus, status)
                .eq(mine, WorkflowInstanceDO::getInitiatorId, loginUser.getUserId())
                .orderByDesc(WorkflowInstanceDO::getCreateTime)
                .orderByDesc(WorkflowInstanceDO::getInstanceId);
        Page<WorkflowInstanceDO> page = baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page, page.getRecords().stream().map(this::toVO).toList());
    }

    @Override
    public WorkflowInstanceVO getRequired(Long instanceId) {
        WorkflowInstanceDO instance = getRequiredDO(instanceId);
        WorkflowInstanceVO view = toVO(instance);
        view.setEvents(eventMapper.selectList(new LambdaQueryWrapper<WorkflowInstanceEventDO>()
                        .eq(WorkflowInstanceEventDO::getInstanceId, instanceId)
                        .orderByAsc(WorkflowInstanceEventDO::getCreateTime)
                        .orderByAsc(WorkflowInstanceEventDO::getEventId))
                .stream().map(this::toEventVO).toList());
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long start(WorkflowInstanceStartRequest request) {
        LoginUser loginUser = requireLoginUser();
        String flowCode = StrUtil.trim(request.getFlowCode());
        WorkflowDefinitionDO definition = definitionMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinitionDO>()
                .eq(WorkflowDefinitionDO::getFlowCode, flowCode)
                .eq(WorkflowDefinitionDO::getPublishStatus, 1));
        if (definition == null || !Objects.equals(definition.getActivityStatus(), 1)) {
            throw new BusinessException("没有可用的已发布流程定义");
        }
        String businessKey = StrUtil.trim(request.getBusinessKey());
        if (baseMapper.selectCount(new LambdaQueryWrapper<WorkflowInstanceDO>()
                .eq(WorkflowInstanceDO::getBusinessKey, businessKey)) > 0) {
            throw new BusinessException("业务标识已存在流程实例");
        }

        Map<String, Object> variables = request.getVariables() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(request.getVariables());
        WorkflowRuntimeResolver.Transition transition = runtimeResolver.start(definition.getModelJson(), variables);
        WorkflowInstanceDO instance = new WorkflowInstanceDO()
                .setDefinitionId(definition.getDefinitionId())
                .setBusinessKey(businessKey)
                .setTitle(StrUtil.trim(request.getTitle()))
                .setFlowCode(definition.getFlowCode())
                .setFlowName(definition.getFlowName())
                .setVersionNo(definition.getVersionNo())
                .setInitiatorId(loginUser.getUserId())
                .setCurrentNodeId(transition.target().id())
                .setCurrentNodeName(transition.target().name())
                .setCurrentNodeType(transition.target().type())
                .setCurrentAssignee(transition.completed() ? null : transition.target().assignee())
                .setStatus(transition.completed() ? "COMPLETED" : RUNNING)
                .setActivityStatus(1)
                .setVariablesJson(JSONUtil.toJsonStr(variables))
                .setModelJson(definition.getModelJson())
                .setEndTime(transition.completed() ? LocalDateTime.now() : null);
        baseMapper.insert(instance);
        taskLifecycleService.createCurrent(instance);
        insertEvent(instance.getInstanceId(), "START", transition.from(), transition.target(), loginUser, null);
        return instance.getInstanceId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void action(Long instanceId, WorkflowInstanceActionRequest request) {
        LoginUser loginUser = requireLoginUser();
        WorkflowInstanceDO instance = getRunning(instanceId);
        requireActive(instance);
        requireAssignee(instance, loginUser);
        WorkflowRuntimeResolver.RuntimeNode from = currentNode(instance);
        taskLifecycleService.requireCompletable(instanceId, from.id());

        if ("REJECT".equals(request.getAction())) {
            updateRunningInstance(instance, from.id(), "REJECTED", null, true);
            taskLifecycleService.archiveCurrent(instanceId, from.id(), loginUser, "REJECT", null, request.getComment());
            insertEvent(instanceId, "REJECT", from, null, loginUser, request.getComment());
            return;
        }

        WorkflowRuntimeResolver.Transition transition = runtimeResolver.advance(
                instance.getModelJson(), instance.getCurrentNodeId(), parseVariables(instance.getVariablesJson()));
        applyTransition(instance, from.id(), transition);
        taskLifecycleService.archiveCurrent(instanceId, from.id(), loginUser, "APPROVE",
                transition.target(), request.getComment());
        taskLifecycleService.createCurrent(instance);
        insertEvent(instanceId, "APPROVE", from, transition.target(), loginUser, request.getComment());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long instanceId, String comment) {
        LoginUser loginUser = requireLoginUser();
        WorkflowInstanceDO instance = getRunning(instanceId);
        if (!Objects.equals(instance.getInitiatorId(), loginUser.getUserId())) {
            throw new BusinessException("只有发起人可以撤回流程");
        }
        WorkflowRuntimeResolver.RuntimeNode from = currentNode(instance);
        updateRunningInstance(instance, from.id(), "CANCELED", null, true);
        insertEvent(instanceId, "CANCEL", from, null, loginUser, comment);
        taskLifecycleService.archiveCurrent(instanceId, from.id(), loginUser, "CANCEL", null, comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void urge(Long instanceId, String content) {
        LoginUser loginUser = requireLoginUser();
        WorkflowInstanceDO instance = getRunning(instanceId);
        boolean allowed = Objects.equals(instance.getInitiatorId(), loginUser.getUserId())
                || loginUser.getRoles().contains("admin");
        if (!allowed) {
            throw new BusinessException("只有发起人或管理员可以催办流程");
        }
        requireActive(instance);
        String urgeContent = StrUtil.trim(content);
        if (StrUtil.isBlank(urgeContent)) {
            throw new BusinessException("催办内容不能为空");
        }
        long recentCount = eventMapper.selectCount(new LambdaQueryWrapper<WorkflowInstanceEventDO>()
                .eq(WorkflowInstanceEventDO::getInstanceId, instanceId)
                .eq(WorkflowInstanceEventDO::getEventType, "URGE")
                .eq(WorkflowInstanceEventDO::getOperatorId, loginUser.getUserId())
                .ge(WorkflowInstanceEventDO::getCreateTime, LocalDateTime.now().minusMinutes(5)));
        if (recentCount > 0) {
            throw new BusinessException("同一流程5分钟内只能催办一次");
        }

        WorkflowTaskDO task = taskLifecycleService.getCurrent(instanceId, instance.getCurrentNodeId());
        List<Long> receiverIds = task.getAssigneeUserId() == null
                ? List.of() : List.of(task.getAssigneeUserId());
        List<String> roleKeys = task.getAssignee() != null && task.getAssignee().startsWith("role:")
                ? List.of(task.getAssignee().substring(5)) : List.of();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("instanceId", instanceId);
        data.put("taskId", task.getTaskId());
        data.put("businessKey", instance.getBusinessKey());
        notificationPublisher.orElseThrow(() -> new BusinessException("系统未配置用户消息发布器"))
                .publish(new NzUserNotification(
                        loginUser.getUserId(),
                        receiverIds,
                        roleKeys,
                        "workflow",
                        "urge",
                        "workflow",
                        "流程催办：" + instance.getFlowName(),
                        instance.getTitle() + " · " + instance.getCurrentNodeName(),
                        urgeContent,
                        JSONUtil.toJsonStr(data),
                        "/workflow/task"
                ));
        WorkflowRuntimeResolver.RuntimeNode current = currentNode(instance);
        insertEvent(instanceId, "URGE", current, current, loginUser, urgeContent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminate(Long instanceId, String comment) {
        LoginUser loginUser = requireLoginUser();
        WorkflowInstanceDO instance = getRunning(instanceId);
        WorkflowRuntimeResolver.RuntimeNode from = currentNode(instance);
        updateRunningInstance(instance, from.id(), "TERMINATED", null, true);
        insertEvent(instanceId, "TERMINATE", from, null, loginUser, comment);
        taskLifecycleService.archiveCurrent(instanceId, from.id(), loginUser, "TERMINATE", null, comment);
    }

    @Override
    public void setActive(Long instanceId, boolean active) {
        WorkflowInstanceDO instance = getRunning(instanceId);
        int rows = baseMapper.update(null, new LambdaUpdateWrapper<WorkflowInstanceDO>()
                .eq(WorkflowInstanceDO::getInstanceId, instanceId)
                .eq(WorkflowInstanceDO::getStatus, RUNNING)
                .set(WorkflowInstanceDO::getActivityStatus, active ? 1 : 0));
        if (rows != 1) {
            throw new BusinessException("流程实例状态已变化，请刷新后重试");
        }
        instance.setActivityStatus(active ? 1 : 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long instanceId) {
        WorkflowInstanceDO instance = getRequiredDO(instanceId);
        if (RUNNING.equals(instance.getStatus())) {
            throw new BusinessException("运行中的流程实例不能删除");
        }
        eventMapper.delete(new LambdaQueryWrapper<WorkflowInstanceEventDO>()
                .eq(WorkflowInstanceEventDO::getInstanceId, instanceId));
        taskLifecycleService.deleteByInstance(instanceId);
        baseMapper.deleteById(instanceId);
    }

    private void applyTransition(WorkflowInstanceDO instance, String expectedNodeId,
                                 WorkflowRuntimeResolver.Transition transition) {
        WorkflowRuntimeResolver.RuntimeNode target = transition.target();
        instance.setCurrentNodeId(target.id());
        instance.setCurrentNodeName(target.name());
        instance.setCurrentNodeType(target.type());
        instance.setCurrentAssignee(transition.completed() ? null : target.assignee());
        instance.setStatus(transition.completed() ? "COMPLETED" : RUNNING);
        instance.setEndTime(transition.completed() ? LocalDateTime.now() : null);
        updateRunningInstance(instance, expectedNodeId, instance.getStatus(), instance.getCurrentAssignee(),
                transition.completed());
    }

    private void updateRunningInstance(WorkflowInstanceDO instance, String expectedNodeId, String status,
                                       String assignee, boolean ended) {
        LambdaUpdateWrapper<WorkflowInstanceDO> wrapper = new LambdaUpdateWrapper<WorkflowInstanceDO>()
                .eq(WorkflowInstanceDO::getInstanceId, instance.getInstanceId())
                .eq(WorkflowInstanceDO::getStatus, RUNNING)
                .eq(WorkflowInstanceDO::getCurrentNodeId, expectedNodeId)
                .set(WorkflowInstanceDO::getCurrentNodeId, instance.getCurrentNodeId())
                .set(WorkflowInstanceDO::getCurrentNodeName, instance.getCurrentNodeName())
                .set(WorkflowInstanceDO::getCurrentNodeType, instance.getCurrentNodeType())
                .set(WorkflowInstanceDO::getCurrentAssignee, assignee)
                .set(WorkflowInstanceDO::getStatus, status)
                .set(ended, WorkflowInstanceDO::getEndTime, LocalDateTime.now());
        if (baseMapper.update(null, wrapper) != 1) {
            throw new BusinessException("流程实例已被其他操作更新，请刷新后重试");
        }
        instance.setStatus(status);
        instance.setCurrentAssignee(assignee);
        if (ended) {
            instance.setEndTime(LocalDateTime.now());
        }
    }

    private void requireAssignee(WorkflowInstanceDO instance, LoginUser loginUser) {
        String assignee = instance.getCurrentAssignee();
        boolean allowed = loginUser.getRoles().contains("admin")
                || "initiator".equals(assignee) && Objects.equals(instance.getInitiatorId(), loginUser.getUserId())
                || assignee != null && assignee.startsWith("user:")
                && assignee.substring(5).equals(String.valueOf(loginUser.getUserId()))
                || assignee != null && assignee.startsWith("role:")
                && loginUser.getRoles().contains(assignee.substring(5));
        if (!allowed) {
            throw new BusinessException("当前用户不是该节点办理人");
        }
    }

    private WorkflowInstanceDO getRunning(Long instanceId) {
        WorkflowInstanceDO instance = getRequiredDO(instanceId);
        if (!RUNNING.equals(instance.getStatus())) {
            throw new BusinessException("流程实例已经结束");
        }
        return instance;
    }

    private void requireActive(WorkflowInstanceDO instance) {
        if (!Objects.equals(instance.getActivityStatus(), 1)) {
            throw new BusinessException("流程实例已挂起");
        }
    }

    private WorkflowInstanceDO getRequiredDO(Long instanceId) {
        WorkflowInstanceDO instance = baseMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("流程实例不存在");
        }
        return instance;
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = loginUserContext.getLoginUserOrNull();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BusinessException("当前用户未登录");
        }
        return loginUser;
    }

    private Map<String, Object> parseVariables(String variablesJson) {
        return new LinkedHashMap<>(JSONUtil.parseObj(StrUtil.blankToDefault(variablesJson, "{}")));
    }

    private WorkflowRuntimeResolver.RuntimeNode currentNode(WorkflowInstanceDO instance) {
        return new WorkflowRuntimeResolver.RuntimeNode(instance.getCurrentNodeId(), instance.getCurrentNodeName(),
                instance.getCurrentNodeType(), instance.getCurrentAssignee());
    }

    private void insertEvent(Long instanceId, String eventType, WorkflowRuntimeResolver.RuntimeNode from,
                             WorkflowRuntimeResolver.RuntimeNode to, LoginUser operator, String comment) {
        eventMapper.insert(new WorkflowInstanceEventDO()
                .setInstanceId(instanceId)
                .setEventType(eventType)
                .setFromNodeId(from == null ? null : from.id())
                .setFromNodeName(from == null ? null : from.name())
                .setToNodeId(to == null ? null : to.id())
                .setToNodeName(to == null ? null : to.name())
                .setOperatorId(operator.getUserId())
                .setOperatorName(operator.getUsername())
                .setComment(StrUtil.trim(comment)));
    }

    private WorkflowInstanceVO toVO(WorkflowInstanceDO instance) {
        return new WorkflowInstanceVO()
                .setInstanceId(instance.getInstanceId())
                .setDefinitionId(instance.getDefinitionId())
                .setBusinessKey(instance.getBusinessKey())
                .setTitle(instance.getTitle())
                .setFlowCode(instance.getFlowCode())
                .setFlowName(instance.getFlowName())
                .setVersionNo(instance.getVersionNo())
                .setInitiatorId(instance.getInitiatorId())
                .setCurrentNodeId(instance.getCurrentNodeId())
                .setCurrentNodeName(instance.getCurrentNodeName())
                .setCurrentNodeType(instance.getCurrentNodeType())
                .setCurrentAssignee(instance.getCurrentAssignee())
                .setStatus(instance.getStatus())
                .setActivityStatus(instance.getActivityStatus())
                .setVariablesJson(instance.getVariablesJson())
                .setCreateTime(instance.getCreateTime())
                .setUpdateTime(instance.getUpdateTime())
                .setEndTime(instance.getEndTime());
    }

    private WorkflowInstanceEventVO toEventVO(WorkflowInstanceEventDO event) {
        return new WorkflowInstanceEventVO()
                .setEventId(event.getEventId())
                .setEventType(event.getEventType())
                .setFromNodeId(event.getFromNodeId())
                .setFromNodeName(event.getFromNodeName())
                .setToNodeId(event.getToNodeId())
                .setToNodeName(event.getToNodeName())
                .setOperatorId(event.getOperatorId())
                .setOperatorName(event.getOperatorName())
                .setComment(event.getComment())
                .setCreateTime(event.getCreateTime());
    }
}
