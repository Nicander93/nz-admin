package com.nz.admin.common.module;

import java.util.List;

/**
 * 将工作流办理人表达式解析为有效用户 ID。
 */
public interface NzWorkflowAssigneeResolver {

    List<String> resolveUserIds(List<String> assigneeExpressions);
}
