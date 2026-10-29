package com.nz.admin.modules.system.config;

import cn.dev33.satoken.util.SaFoxUtil;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.datascope.core.DataScopeContext;
import com.nz.admin.framework.realtime.core.RealtimeAccessPolicy;
import com.nz.admin.framework.realtime.core.RealtimeDeliveryKind;
import com.nz.admin.framework.realtime.core.RealtimeMessage;
import com.nz.admin.framework.realtime.core.RealtimePrincipal;
import com.nz.admin.framework.realtime.core.RealtimeScope;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.service.permission.PermissionService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

import java.util.Set;

/** 长连接按接收人的当前权限授权，不借用发布线程的登录态或租户。 */
@Component
public class SystemRealtimeAccessPolicy implements RealtimeAccessPolicy {
    private static final Set<String> NOTIFICATION_TYPES = Set.of("system-message", "system-message-state");
    private final PermissionService permissions;
    private final CommittedSecurityReader committed;

    public SystemRealtimeAccessPolicy(PermissionService permissions, CommittedSecurityReader committed) {
        this.permissions = permissions;
        this.committed = committed;
    }

    @Override
    public boolean isAuthorized(RealtimePrincipal principal) {
        if (principal == null || !principal.identity().isComplete() || principal.scope() == null) return false;
        String required = principal.scope() == RealtimeScope.USER_NOTIFICATIONS
                ? "system:message:list" : "system:realtime:view";
        try {
            return TenantContextHolder.callWithTenantId(principal.tenantId(), () ->
                    committed.read(() -> DataScopeContext.withoutFilter(() -> permissions.getPermsByUserId(principal.userId())
                            .stream().anyMatch(granted -> SaFoxUtil.vagueMatch(granted, required)))));
        } catch (DataAccessException | TransactionException error) {
            BusinessException failure = new BusinessException(503, "权限状态暂不可用，请稍后重试");
            failure.initCause(error);
            throw failure;
        }
    }

    @Override
    public boolean canReceive(RealtimePrincipal principal, RealtimeDeliveryKind kind, RealtimeMessage message) {
        if (principal == null || principal.scope() == null || message == null) return false;
        if (principal.scope() == RealtimeScope.GENERAL) return true;
        return kind == RealtimeDeliveryKind.USER && NOTIFICATION_TYPES.contains(message.type());
    }
}
