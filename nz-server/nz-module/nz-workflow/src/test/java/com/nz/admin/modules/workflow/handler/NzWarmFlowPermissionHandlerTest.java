package com.nz.admin.modules.workflow.handler;

import com.nz.admin.common.module.NzWorkflowAssigneeResolver;
import com.nz.admin.framework.auth.core.LoginUserContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NzWarmFlowPermissionHandlerTest {

    private final LoginUserContext loginUserContext = mock(LoginUserContext.class);
    private final NzWorkflowAssigneeResolver assigneeResolver = mock(NzWorkflowAssigneeResolver.class);
    private final NzWarmFlowPermissionHandler handler =
            new NzWarmFlowPermissionHandler(loginUserContext, Optional.of(assigneeResolver));

    @Test
    void exposesOnlyCurrentUserIdForTaskMatching() {
        when(loginUserContext.getLoginUserIdOrNull()).thenReturn(7L);

        assertThat(handler.permissions()).containsExactly("7");
        assertThat(handler.getHandler()).isEqualTo("7");
    }

    @Test
    void delegatesStoredAssigneeExpressionsToSystemResolver() {
        List<String> expressions = List.of("user:7", "role:manager");
        when(assigneeResolver.resolveUserIds(expressions)).thenReturn(List.of("7", "8"));

        assertThat(handler.convertPermissions(expressions)).containsExactly("7", "8");
    }

    @Test
    void returnsNoIdentityWhenRequestIsAnonymous() {
        when(loginUserContext.getLoginUserIdOrNull()).thenReturn(null);

        assertThat(handler.permissions()).isEmpty();
        assertThat(handler.getHandler()).isNull();
    }

    @Test
    void failsClosedWhenAssigneeResolverIsUnavailable() {
        NzWarmFlowPermissionHandler standaloneHandler =
                new NzWarmFlowPermissionHandler(loginUserContext, Optional.empty());

        assertThat(standaloneHandler.convertPermissions(List.of("user:7"))).isEmpty();
    }
}
