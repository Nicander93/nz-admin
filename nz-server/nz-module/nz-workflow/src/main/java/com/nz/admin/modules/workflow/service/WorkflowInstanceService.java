package com.nz.admin.modules.workflow.service;

import com.nz.admin.common.core.PageResult;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceActionRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceStartRequest;
import com.nz.admin.modules.workflow.entity.vo.WorkflowInstanceVO;

/**
 * 流程实例服务。
 */
public interface WorkflowInstanceService {

    PageResult<WorkflowInstanceVO> page(Integer pageNum, Integer pageSize, String flowCode, String title,
                                        String businessKey, String status, boolean mine);
    WorkflowInstanceVO getRequired(Long instanceId);
    Long start(WorkflowInstanceStartRequest request);
    void action(Long instanceId, WorkflowInstanceActionRequest request);
    void urge(Long instanceId, String content);
    void cancel(Long instanceId, String comment);
    void terminate(Long instanceId, String comment);
    void setActive(Long instanceId, boolean active);
    void delete(Long instanceId);
}
