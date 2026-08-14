package com.nz.admin.modules.workflow.service;

import com.nz.admin.common.core.PageResult;
import com.nz.admin.modules.workflow.entity.dto.WorkflowDefinitionCopyRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowDefinitionCreateRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowDefinitionUpdateRequest;
import com.nz.admin.modules.workflow.entity.vo.WorkflowDefinitionVO;

/**
 * 流程定义服务。
 */
public interface WorkflowDefinitionService {

    PageResult<WorkflowDefinitionVO> page(Integer pageNum, Integer pageSize, String flowCode, String flowName,
                                          Long categoryId, Integer publishStatus);
    WorkflowDefinitionVO getRequired(Long definitionId);
    Long create(WorkflowDefinitionCreateRequest request);
    void update(WorkflowDefinitionUpdateRequest request);
    void publish(Long definitionId);
    void unpublish(Long definitionId);
    void setActive(Long definitionId, boolean active);
    Long copy(WorkflowDefinitionCopyRequest request);
    Long importJson(String json, Long categoryId);
    String exportJson(Long definitionId);
    void delete(Long definitionId);
}
