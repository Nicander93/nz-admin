package com.nz.admin.modules.workflow.handler;

import com.nz.admin.common.module.NzWorkflowAssigneeResolver;
import com.nz.admin.framework.auth.core.LoginUserContext;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 将 Warm-Flow 办理人权限接入项目登录用户上下文。
 */
@Component
public class NzWarmFlowPermissionHandler implements PermissionHandler {

    private final LoginUserContext loginUserContext;
    private final Optional<NzWorkflowAssigneeResolver> assigneeResolver;

    public NzWarmFlowPermissionHandler(LoginUserContext loginUserContext,
                                       Optional<NzWorkflowAssigneeResolver> assigneeResolver) {
        this.loginUserContext = loginUserContext;
        this.assigneeResolver = assigneeResolver;
    }

    @Override
    public List<String> permissions() {
        String handler = getHandler();
        return handler == null ? List.of() : List.of(handler);
    }

    @Override
    public String getHandler() {
        Long userId = loginUserContext.getLoginUserIdOrNull();
        return userId == null ? null : userId.toString();
    }

    @Override
    public List<String> convertPermissions(List<String> permissions) {
        return assigneeResolver.map(resolver -> resolver.resolveUserIds(permissions))
                .orElseGet(List::of);
    }
}
