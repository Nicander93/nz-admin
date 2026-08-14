package com.nz.admin.modules.workflow.service;

/**
 * 流程实例对定义版本的使用检查边界。
 */
@FunctionalInterface
public interface WorkflowDefinitionUsageChecker {

    boolean isUsed(Long definitionId);
}
