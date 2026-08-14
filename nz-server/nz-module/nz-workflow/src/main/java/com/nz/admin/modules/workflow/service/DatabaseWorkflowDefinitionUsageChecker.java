package com.nz.admin.modules.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowInstanceDO;
import com.nz.admin.modules.workflow.mapper.WorkflowInstanceMapper;
import org.springframework.stereotype.Component;

/**
 * 基于实例表判断定义版本是否已经投入运行。
 */
@Component
public class DatabaseWorkflowDefinitionUsageChecker implements WorkflowDefinitionUsageChecker {

    private final WorkflowInstanceMapper instanceMapper;

    public DatabaseWorkflowDefinitionUsageChecker(WorkflowInstanceMapper instanceMapper) {
        this.instanceMapper = instanceMapper;
    }

    @Override
    public boolean isUsed(Long definitionId) {
        return instanceMapper.selectCount(new LambdaQueryWrapper<WorkflowInstanceDO>()
                .eq(WorkflowInstanceDO::getDefinitionId, definitionId)) > 0;
    }
}
