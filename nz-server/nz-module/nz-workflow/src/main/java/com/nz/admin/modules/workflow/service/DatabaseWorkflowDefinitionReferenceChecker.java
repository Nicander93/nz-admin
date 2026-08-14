package com.nz.admin.modules.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowDefinitionDO;
import com.nz.admin.modules.workflow.mapper.WorkflowDefinitionMapper;
import org.springframework.stereotype.Component;

/**
 * 基于定义表判断分类是否已被引用。
 */
@Component
public class DatabaseWorkflowDefinitionReferenceChecker implements WorkflowDefinitionReferenceChecker {

    private final WorkflowDefinitionMapper definitionMapper;

    public DatabaseWorkflowDefinitionReferenceChecker(WorkflowDefinitionMapper definitionMapper) {
        this.definitionMapper = definitionMapper;
    }

    @Override
    public boolean hasDefinitions(Long categoryId) {
        return definitionMapper.selectCount(new LambdaQueryWrapper<WorkflowDefinitionDO>()
                .eq(WorkflowDefinitionDO::getCategoryId, categoryId)) > 0;
    }
}
