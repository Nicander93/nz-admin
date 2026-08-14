package com.nz.admin.modules.workflow.service;

import com.nz.admin.modules.workflow.entity.dataobject.WorkflowCategoryDO;
import com.nz.admin.modules.workflow.entity.dto.WorkflowCategoryCreateRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowCategoryUpdateRequest;
import com.nz.admin.modules.workflow.entity.vo.WorkflowCategoryVO;

import java.util.List;

/**
 * 流程分类服务。
 */
public interface WorkflowCategoryService {

    List<WorkflowCategoryDO> list(String categoryName, Long parentId);
    List<WorkflowCategoryVO> tree(String categoryName);
    WorkflowCategoryDO getRequired(Long categoryId);
    Long create(WorkflowCategoryCreateRequest request);
    void update(WorkflowCategoryUpdateRequest request);
    void delete(Long categoryId);
}
