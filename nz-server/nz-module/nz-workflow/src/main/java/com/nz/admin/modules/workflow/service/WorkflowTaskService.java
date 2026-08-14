package com.nz.admin.modules.workflow.service;

import com.nz.admin.common.core.PageResult;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceActionRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowTaskCopyRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowTaskDelegateRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowTaskTransferRequest;
import com.nz.admin.modules.workflow.entity.vo.WorkflowTaskVO;

/** 流程任务服务。 */
public interface WorkflowTaskService {

    PageResult<WorkflowTaskVO> pageTodo(Integer pageNum, Integer pageSize, String nodeName);
    PageResult<WorkflowTaskVO> pageDone(Integer pageNum, Integer pageSize, String nodeName, String action);
    PageResult<WorkflowTaskVO> pageCopy(Integer pageNum, Integer pageSize, Integer readStatus);
    WorkflowTaskVO getRequired(Long taskId);
    void action(Long taskId, WorkflowInstanceActionRequest request);
    void transfer(Long taskId, WorkflowTaskTransferRequest request);
    void delegate(Long taskId, WorkflowTaskDelegateRequest request);
    void resolveDelegation(Long taskId, String comment);
    void copy(Long taskId, WorkflowTaskCopyRequest request);
    void markCopyRead(Long copyId);
}
