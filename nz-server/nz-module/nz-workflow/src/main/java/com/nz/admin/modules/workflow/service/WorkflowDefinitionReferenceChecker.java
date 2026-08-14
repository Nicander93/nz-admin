package com.nz.admin.modules.workflow.service;

/**
 * 流程定义对分类的引用检查边界。
 */
@FunctionalInterface
public interface WorkflowDefinitionReferenceChecker {

    boolean hasDefinitions(Long categoryId);
}
