package com.nz.admin.modules.workflow.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.core.PageResult;
import com.nz.admin.framework.auth.core.LoginUser;
import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowHistoryTaskDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowInstanceDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowTaskCopyDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowTaskDO;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceActionRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowTaskCopyRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowTaskDelegateRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowTaskTransferRequest;
import com.nz.admin.modules.workflow.entity.vo.WorkflowTaskVO;
import com.nz.admin.modules.workflow.mapper.WorkflowHistoryTaskMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowInstanceMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowTaskCopyMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 流程任务服务实现。 */
@Service
public class WorkflowTaskServiceImpl extends ServiceImpl<WorkflowTaskMapper, WorkflowTaskDO>
        implements WorkflowTaskService {

    private final WorkflowHistoryTaskMapper historyTaskMapper;
    private final WorkflowTaskCopyMapper taskCopyMapper;
    private final WorkflowInstanceMapper instanceMapper;
    private final WorkflowInstanceService instanceService;
    private final WorkflowTaskLifecycleService taskLifecycleService;
    private final LoginUserContext loginUserContext;

    public WorkflowTaskServiceImpl(WorkflowHistoryTaskMapper historyTaskMapper,
                                   WorkflowTaskCopyMapper taskCopyMapper,
                                   WorkflowInstanceMapper instanceMapper,
                                   WorkflowInstanceService instanceService,
                                   WorkflowTaskLifecycleService taskLifecycleService,
                                   LoginUserContext loginUserContext) {
        this.historyTaskMapper = historyTaskMapper;
        this.taskCopyMapper = taskCopyMapper;
        this.instanceMapper = instanceMapper;
        this.instanceService = instanceService;
        this.taskLifecycleService = taskLifecycleService;
        this.loginUserContext = loginUserContext;
    }

    @Override
    public PageResult<WorkflowTaskVO> pageTodo(Integer pageNum, Integer pageSize, String nodeName) {
        LoginUser loginUser = requireLoginUser();
        List<String> roleAssignees = loginUser.getRoles().stream().map(role -> "role:" + role).toList();
        LambdaQueryWrapper<WorkflowTaskDO> wrapper = new LambdaQueryWrapper<WorkflowTaskDO>()
                .like(StrUtil.isNotBlank(nodeName), WorkflowTaskDO::getNodeName, StrUtil.trim(nodeName))
                .and(scope -> {
                    scope.eq(WorkflowTaskDO::getAssigneeUserId, loginUser.getUserId());
                    if (!roleAssignees.isEmpty()) {
                        scope.or().in(WorkflowTaskDO::getAssignee, roleAssignees);
                    }
                })
                .orderByDesc(WorkflowTaskDO::getCreateTime)
                .orderByDesc(WorkflowTaskDO::getTaskId);
        Page<WorkflowTaskDO> page = baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, WorkflowInstanceDO> instances = loadInstances(
                page.getRecords().stream().map(WorkflowTaskDO::getInstanceId).toList());
        return PageResult.of(page, page.getRecords().stream()
                .map(task -> toVO(task, instances.get(task.getInstanceId()))).toList());
    }

    @Override
    public PageResult<WorkflowTaskVO> pageDone(Integer pageNum, Integer pageSize, String nodeName, String action) {
        LoginUser loginUser = requireLoginUser();
        LambdaQueryWrapper<WorkflowHistoryTaskDO> wrapper = new LambdaQueryWrapper<WorkflowHistoryTaskDO>()
                .eq(WorkflowHistoryTaskDO::getOperatorId, loginUser.getUserId())
                .like(StrUtil.isNotBlank(nodeName), WorkflowHistoryTaskDO::getNodeName, StrUtil.trim(nodeName))
                .eq(StrUtil.isNotBlank(action), WorkflowHistoryTaskDO::getAction, action)
                .orderByDesc(WorkflowHistoryTaskDO::getCreateTime)
                .orderByDesc(WorkflowHistoryTaskDO::getHistoryId);
        Page<WorkflowHistoryTaskDO> page = historyTaskMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, WorkflowInstanceDO> instances = loadInstances(
                page.getRecords().stream().map(WorkflowHistoryTaskDO::getInstanceId).toList());
        return PageResult.of(page, page.getRecords().stream()
                .map(task -> toVO(task, instances.get(task.getInstanceId()))).toList());
    }

    @Override
    public PageResult<WorkflowTaskVO> pageCopy(Integer pageNum, Integer pageSize, Integer readStatus) {
        LoginUser loginUser = requireLoginUser();
        LambdaQueryWrapper<WorkflowTaskCopyDO> wrapper = new LambdaQueryWrapper<WorkflowTaskCopyDO>()
                .eq(WorkflowTaskCopyDO::getReceiverId, loginUser.getUserId())
                .eq(readStatus != null, WorkflowTaskCopyDO::getReadStatus, readStatus)
                .orderByDesc(WorkflowTaskCopyDO::getCreateTime)
                .orderByDesc(WorkflowTaskCopyDO::getCopyId);
        Page<WorkflowTaskCopyDO> page = taskCopyMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, WorkflowInstanceDO> instances = loadInstances(
                page.getRecords().stream().map(WorkflowTaskCopyDO::getInstanceId).toList());
        return PageResult.of(page, page.getRecords().stream()
                .map(copy -> toVO(copy, instances.get(copy.getInstanceId()))).toList());
    }

    @Override
    public WorkflowTaskVO getRequired(Long taskId) {
        WorkflowTaskDO task = getAuthorizedTask(taskId, requireLoginUser());
        return toVO(task, instanceMapper.selectById(task.getInstanceId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void action(Long taskId, WorkflowInstanceActionRequest request) {
        WorkflowTaskDO task = getAuthorizedTask(taskId, requireLoginUser());
        if (Objects.equals(task.getDelegationStatus(), 1)) {
            throw new BusinessException("受托任务应先完成委派并归还原办理人");
        }
        instanceService.action(task.getInstanceId(), request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long taskId, WorkflowTaskTransferRequest request) {
        LoginUser loginUser = requireLoginUser();
        WorkflowTaskDO task = getAuthorizedTask(taskId, loginUser);
        if (Objects.equals(request.getTargetUserId(), loginUser.getUserId())) {
            throw new BusinessException("不能把任务转办给自己");
        }
        if (Objects.equals(task.getDelegationStatus(), 1)) {
            throw new BusinessException("委派中的任务不能转办");
        }
        updateInstanceAssignee(task, "user:" + request.getTargetUserId());
        taskLifecycleService.transfer(task, loginUser, request.getTargetUserId(), request.getComment());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegate(Long taskId, WorkflowTaskDelegateRequest request) {
        LoginUser loginUser = requireLoginUser();
        WorkflowTaskDO task = getAuthorizedTask(taskId, loginUser);
        if (Objects.equals(request.getTargetUserId(), loginUser.getUserId())) {
            throw new BusinessException("不能把任务委派给自己");
        }
        if (Objects.equals(task.getDelegationStatus(), 1)) {
            throw new BusinessException("任务已经处于委派状态");
        }
        updateInstanceAssignee(task, "user:" + request.getTargetUserId());
        taskLifecycleService.delegate(task, loginUser, request.getTargetUserId(), request.getComment());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveDelegation(Long taskId, String comment) {
        LoginUser loginUser = requireLoginUser();
        WorkflowTaskDO task = getAuthorizedTask(taskId, loginUser);
        if (!Objects.equals(task.getDelegationStatus(), 1) || StrUtil.isBlank(task.getOwnerAssignee())) {
            throw new BusinessException("任务不处于可归还的委派状态");
        }
        updateInstanceAssignee(task, task.getOwnerAssignee());
        taskLifecycleService.resolveDelegation(task, loginUser, comment);
    }

    private void updateInstanceAssignee(WorkflowTaskDO task, String targetAssignee) {
        int rows = instanceMapper.update(null, new LambdaUpdateWrapper<WorkflowInstanceDO>()
                .eq(WorkflowInstanceDO::getInstanceId, task.getInstanceId())
                .eq(WorkflowInstanceDO::getStatus, "RUNNING")
                .eq(WorkflowInstanceDO::getActivityStatus, 1)
                .eq(WorkflowInstanceDO::getCurrentNodeId, task.getNodeId())
                .eq(WorkflowInstanceDO::getCurrentAssignee, task.getAssignee())
                .set(WorkflowInstanceDO::getCurrentAssignee, targetAssignee));
        if (rows != 1) {
            throw new BusinessException("流程实例状态已变化，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copy(Long taskId, WorkflowTaskCopyRequest request) {
        LoginUser loginUser = requireLoginUser();
        WorkflowTaskDO task = getAuthorizedTask(taskId, loginUser);
        Set<Long> receiverIds = new LinkedHashSet<>(request.getReceiverIds());
        receiverIds.remove(loginUser.getUserId());
        if (receiverIds.isEmpty()) {
            throw new BusinessException("抄送人不能只有当前用户");
        }
        for (Long receiverId : receiverIds) {
            long existing = taskCopyMapper.selectCount(new LambdaQueryWrapper<WorkflowTaskCopyDO>()
                    .eq(WorkflowTaskCopyDO::getTaskId, taskId)
                    .eq(WorkflowTaskCopyDO::getReceiverId, receiverId));
            if (existing == 0) {
                taskCopyMapper.insert(new WorkflowTaskCopyDO()
                        .setTaskId(taskId)
                        .setInstanceId(task.getInstanceId())
                        .setReceiverId(receiverId)
                        .setSenderId(loginUser.getUserId())
                        .setSenderName(loginUser.getUsername())
                        .setComment(StrUtil.trim(request.getComment()))
                        .setReadStatus(0));
            }
        }
    }

    @Override
    public void markCopyRead(Long copyId) {
        LoginUser loginUser = requireLoginUser();
        int rows = taskCopyMapper.update(null, new LambdaUpdateWrapper<WorkflowTaskCopyDO>()
                .eq(WorkflowTaskCopyDO::getCopyId, copyId)
                .eq(WorkflowTaskCopyDO::getReceiverId, loginUser.getUserId())
                .eq(WorkflowTaskCopyDO::getReadStatus, 0)
                .set(WorkflowTaskCopyDO::getReadStatus, 1)
                .set(WorkflowTaskCopyDO::getReadTime, LocalDateTime.now()));
        if (rows == 0 && taskCopyMapper.selectCount(new LambdaQueryWrapper<WorkflowTaskCopyDO>()
                .eq(WorkflowTaskCopyDO::getCopyId, copyId)
                .eq(WorkflowTaskCopyDO::getReceiverId, loginUser.getUserId())
                .eq(WorkflowTaskCopyDO::getReadStatus, 1)) == 0) {
            throw new BusinessException("抄送记录不存在");
        }
    }

    private WorkflowTaskDO getAuthorizedTask(Long taskId, LoginUser loginUser) {
        WorkflowTaskDO task = baseMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("待办任务不存在");
        }
        boolean allowed = Objects.equals(task.getAssigneeUserId(), loginUser.getUserId())
                || loginUser.getRoles().stream().anyMatch(role -> ("role:" + role).equals(task.getAssignee()))
                || loginUser.getRoles().contains("admin");
        if (!allowed) {
            throw new BusinessException("当前用户不是该任务办理人");
        }
        return task;
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = loginUserContext.getLoginUserOrNull();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BusinessException("当前用户未登录");
        }
        return loginUser;
    }

    private Map<Long, WorkflowInstanceDO> loadInstances(Collection<Long> instanceIds) {
        if (instanceIds.isEmpty()) {
            return Map.of();
        }
        return instanceMapper.selectBatchIds(new LinkedHashSet<>(instanceIds)).stream()
                .collect(Collectors.toMap(WorkflowInstanceDO::getInstanceId, Function.identity(),
                        (first, ignored) -> first, LinkedHashMap::new));
    }

    private WorkflowTaskVO toVO(WorkflowTaskDO task, WorkflowInstanceDO instance) {
        return baseVO(task.getInstanceId(), task.getDefinitionId(), instance)
                .setTaskId(task.getTaskId())
                .setNodeId(task.getNodeId())
                .setNodeName(task.getNodeName())
                .setAssignee(task.getAssignee())
                .setOwnerAssignee(task.getOwnerAssignee())
                .setDelegationStatus(task.getDelegationStatus())
                .setCreateTime(task.getCreateTime())
                .setUpdateTime(task.getUpdateTime());
    }

    private WorkflowTaskVO toVO(WorkflowHistoryTaskDO task, WorkflowInstanceDO instance) {
        return baseVO(task.getInstanceId(), task.getDefinitionId(), instance)
                .setTaskId(task.getTaskId())
                .setHistoryId(task.getHistoryId())
                .setNodeId(task.getNodeId())
                .setNodeName(task.getNodeName())
                .setAssignee(task.getAssignee())
                .setOperatorId(task.getOperatorId())
                .setOperatorName(task.getOperatorName())
                .setAction(task.getAction())
                .setTargetNodeName(task.getTargetNodeName())
                .setTargetAssignee(task.getTargetAssignee())
                .setComment(task.getComment())
                .setCreateTime(task.getTaskCreateTime())
                .setUpdateTime(task.getCreateTime());
    }

    private WorkflowTaskVO toVO(WorkflowTaskCopyDO copy, WorkflowInstanceDO instance) {
        return baseVO(copy.getInstanceId(), instance == null ? null : instance.getDefinitionId(), instance)
                .setTaskId(copy.getTaskId())
                .setCopyId(copy.getCopyId())
                .setOperatorId(copy.getSenderId())
                .setOperatorName(copy.getSenderName())
                .setComment(copy.getComment())
                .setReadStatus(copy.getReadStatus())
                .setCreateTime(copy.getCreateTime())
                .setUpdateTime(copy.getUpdateTime());
    }

    private WorkflowTaskVO baseVO(Long instanceId, Long definitionId, WorkflowInstanceDO instance) {
        WorkflowTaskVO view = new WorkflowTaskVO()
                .setInstanceId(instanceId)
                .setDefinitionId(definitionId);
        if (instance != null) {
            view.setBusinessKey(instance.getBusinessKey())
                    .setTitle(instance.getTitle())
                    .setFlowCode(instance.getFlowCode())
                    .setFlowName(instance.getFlowName())
                    .setVersionNo(instance.getVersionNo());
        }
        return view;
    }
}
