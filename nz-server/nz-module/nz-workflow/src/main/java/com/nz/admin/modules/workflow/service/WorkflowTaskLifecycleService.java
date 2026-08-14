package com.nz.admin.modules.workflow.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.auth.core.LoginUser;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowHistoryTaskDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowInstanceDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowTaskCopyDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowTaskDO;
import com.nz.admin.modules.workflow.mapper.WorkflowHistoryTaskMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowTaskCopyMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowTaskMapper;
import org.springframework.stereotype.Service;
import java.util.Objects;

/** 维护实例状态变化对应的当前任务和历史任务。 */
@Service
public class WorkflowTaskLifecycleService {

    private final WorkflowTaskMapper taskMapper;
    private final WorkflowHistoryTaskMapper historyTaskMapper;
    private final WorkflowTaskCopyMapper taskCopyMapper;

    public WorkflowTaskLifecycleService(WorkflowTaskMapper taskMapper,
                                        WorkflowHistoryTaskMapper historyTaskMapper,
                                        WorkflowTaskCopyMapper taskCopyMapper) {
        this.taskMapper = taskMapper;
        this.historyTaskMapper = historyTaskMapper;
        this.taskCopyMapper = taskCopyMapper;
    }

    public void createCurrent(WorkflowInstanceDO instance) {
        if (!"RUNNING".equals(instance.getStatus())) {
            return;
        }
        String assignee = instance.getCurrentAssignee();
        WorkflowTaskDO task = new WorkflowTaskDO()
                .setDefinitionId(instance.getDefinitionId())
                .setInstanceId(instance.getInstanceId())
                .setNodeId(instance.getCurrentNodeId())
                .setNodeName(instance.getCurrentNodeName())
                .setAssignee(assignee)
                .setAssigneeUserId(resolveAssigneeUserId(assignee, instance.getInitiatorId()))
                .setDelegationStatus(0);
        taskMapper.insert(task);
    }

    public WorkflowTaskDO getCurrent(Long instanceId, String nodeId) {
        WorkflowTaskDO task = taskMapper.selectOne(new LambdaQueryWrapper<WorkflowTaskDO>()
                .eq(WorkflowTaskDO::getInstanceId, instanceId)
                .eq(WorkflowTaskDO::getNodeId, nodeId));
        if (task == null) {
            throw new BusinessException("当前节点待办任务不存在");
        }
        return task;
    }

    /** 委派任务必须先由受托人完成并归还，原办理人才能推动流程。 */
    public void requireCompletable(Long instanceId, String nodeId) {
        WorkflowTaskDO task = getCurrent(instanceId, nodeId);
        if (Objects.equals(task.getDelegationStatus(), 1)) {
            throw new BusinessException("受托任务应先完成委派并归还原办理人");
        }
    }

    public void archiveCurrent(Long instanceId, String nodeId, LoginUser operator, String action,
                               WorkflowRuntimeResolver.RuntimeNode target, String comment) {
        WorkflowTaskDO task = getCurrent(instanceId, nodeId);
        historyTaskMapper.insert(toHistory(task, operator, action, target,
                target == null ? null : target.assignee(), comment));
        if (taskMapper.deleteById(task.getTaskId()) != 1) {
            throw new BusinessException("待办任务已被其他操作处理，请刷新后重试");
        }
    }

    public void transfer(WorkflowTaskDO task, LoginUser operator, Long targetUserId, String comment) {
        String targetAssignee = "user:" + targetUserId;
        WorkflowRuntimeResolver.RuntimeNode target = new WorkflowRuntimeResolver.RuntimeNode(
                task.getNodeId(), task.getNodeName(), "task", targetAssignee);
        historyTaskMapper.insert(toHistory(task, operator, "TRANSFER", target, targetAssignee, comment));
        int rows = taskMapper.update(null, new LambdaUpdateWrapper<WorkflowTaskDO>()
                .eq(WorkflowTaskDO::getTaskId, task.getTaskId())
                .eq(WorkflowTaskDO::getAssignee, task.getAssignee())
                .set(WorkflowTaskDO::getAssignee, targetAssignee)
                .set(WorkflowTaskDO::getAssigneeUserId, targetUserId)
                .set(WorkflowTaskDO::getOwnerAssignee, null)
                .set(WorkflowTaskDO::getOwnerUserId, null)
                .set(WorkflowTaskDO::getDelegationStatus, 0));
        if (rows != 1) {
            throw new BusinessException("待办任务已被其他操作处理，请刷新后重试");
        }
        task.setAssignee(targetAssignee);
        task.setAssigneeUserId(targetUserId);
        task.setOwnerAssignee(null);
        task.setOwnerUserId(null);
        task.setDelegationStatus(0);
    }

    public void delegate(WorkflowTaskDO task, LoginUser operator, Long targetUserId, String comment) {
        if (!Objects.equals(task.getDelegationStatus(), 0)) {
            throw new BusinessException("任务已经处于委派状态");
        }
        String targetAssignee = "user:" + targetUserId;
        WorkflowRuntimeResolver.RuntimeNode target = new WorkflowRuntimeResolver.RuntimeNode(
                task.getNodeId(), task.getNodeName(), "task", targetAssignee);
        historyTaskMapper.insert(toHistory(task, operator, "DELEGATE", target, targetAssignee, comment));
        int rows = taskMapper.update(null, new LambdaUpdateWrapper<WorkflowTaskDO>()
                .eq(WorkflowTaskDO::getTaskId, task.getTaskId())
                .eq(WorkflowTaskDO::getAssignee, task.getAssignee())
                .eq(WorkflowTaskDO::getDelegationStatus, 0)
                .set(WorkflowTaskDO::getOwnerAssignee, task.getAssignee())
                .set(WorkflowTaskDO::getOwnerUserId, task.getAssigneeUserId())
                .set(WorkflowTaskDO::getAssignee, targetAssignee)
                .set(WorkflowTaskDO::getAssigneeUserId, targetUserId)
                .set(WorkflowTaskDO::getDelegationStatus, 1));
        if (rows != 1) {
            throw new BusinessException("待办任务已被其他操作处理，请刷新后重试");
        }
        task.setOwnerAssignee(task.getAssignee());
        task.setOwnerUserId(task.getAssigneeUserId());
        task.setAssignee(targetAssignee);
        task.setAssigneeUserId(targetUserId);
        task.setDelegationStatus(1);
    }

    public void resolveDelegation(WorkflowTaskDO task, LoginUser operator, String comment) {
        if (!Objects.equals(task.getDelegationStatus(), 1) || StrUtil.isBlank(task.getOwnerAssignee())) {
            throw new BusinessException("任务不处于可归还的委派状态");
        }
        String ownerAssignee = task.getOwnerAssignee();
        Long ownerUserId = task.getOwnerUserId();
        WorkflowRuntimeResolver.RuntimeNode target = new WorkflowRuntimeResolver.RuntimeNode(
                task.getNodeId(), task.getNodeName(), "task", ownerAssignee);
        historyTaskMapper.insert(toHistory(task, operator, "RESOLVE", target, ownerAssignee, comment));
        int rows = taskMapper.update(null, new LambdaUpdateWrapper<WorkflowTaskDO>()
                .eq(WorkflowTaskDO::getTaskId, task.getTaskId())
                .eq(WorkflowTaskDO::getAssignee, task.getAssignee())
                .eq(WorkflowTaskDO::getDelegationStatus, 1)
                .set(WorkflowTaskDO::getAssignee, ownerAssignee)
                .set(WorkflowTaskDO::getAssigneeUserId, ownerUserId)
                .set(WorkflowTaskDO::getOwnerAssignee, null)
                .set(WorkflowTaskDO::getOwnerUserId, null)
                .set(WorkflowTaskDO::getDelegationStatus, 0));
        if (rows != 1) {
            throw new BusinessException("待办任务已被其他操作处理，请刷新后重试");
        }
        task.setAssignee(ownerAssignee);
        task.setAssigneeUserId(ownerUserId);
        task.setOwnerAssignee(null);
        task.setOwnerUserId(null);
        task.setDelegationStatus(0);
    }

    public void deleteByInstance(Long instanceId) {
        taskMapper.delete(new LambdaQueryWrapper<WorkflowTaskDO>()
                .eq(WorkflowTaskDO::getInstanceId, instanceId));
        historyTaskMapper.delete(new LambdaQueryWrapper<WorkflowHistoryTaskDO>()
                .eq(WorkflowHistoryTaskDO::getInstanceId, instanceId));
        taskCopyMapper.delete(new LambdaQueryWrapper<WorkflowTaskCopyDO>()
                .eq(WorkflowTaskCopyDO::getInstanceId, instanceId));
    }

    private WorkflowHistoryTaskDO toHistory(WorkflowTaskDO task, LoginUser operator, String action,
                                            WorkflowRuntimeResolver.RuntimeNode target,
                                            String targetAssignee, String comment) {
        return new WorkflowHistoryTaskDO()
                .setTaskId(task.getTaskId())
                .setDefinitionId(task.getDefinitionId())
                .setInstanceId(task.getInstanceId())
                .setNodeId(task.getNodeId())
                .setNodeName(task.getNodeName())
                .setAssignee(task.getAssignee())
                .setOperatorId(operator.getUserId())
                .setOperatorName(operator.getUsername())
                .setAction(action)
                .setTargetNodeId(target == null ? null : target.id())
                .setTargetNodeName(target == null ? null : target.name())
                .setTargetAssignee(targetAssignee)
                .setComment(StrUtil.trim(comment))
                .setTaskCreateTime(task.getCreateTime());
    }

    private Long resolveAssigneeUserId(String assignee, Long initiatorId) {
        if ("initiator".equals(assignee)) {
            return initiatorId;
        }
        if (assignee != null && assignee.startsWith("user:")) {
            try {
                return Long.valueOf(assignee.substring(5));
            } catch (NumberFormatException exception) {
                throw new BusinessException("流程节点办理人 user 表达式不正确");
            }
        }
        return null;
    }
}
