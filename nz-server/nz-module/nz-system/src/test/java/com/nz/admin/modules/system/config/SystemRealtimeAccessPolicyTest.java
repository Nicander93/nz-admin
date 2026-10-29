package com.nz.admin.modules.system.config;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.realtime.core.*;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.service.permission.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemRealtimeAccessPolicyTest {
    private final PermissionService permissions = mock(PermissionService.class);
    private final org.springframework.transaction.PlatformTransactionManager transactions = mock(org.springframework.transaction.PlatformTransactionManager.class);
    private final SystemRealtimeAccessPolicy policy = new SystemRealtimeAccessPolicy(permissions, new CommittedSecurityReader(transactions));

    @org.junit.jupiter.api.BeforeEach
    void transactionStatus() {
        when(transactions.getTransaction(any())).thenReturn(mock(org.springframework.transaction.TransactionStatus.class));
    }
    private final RealtimePrincipal notifications = new RealtimePrincipal(7L, 3L, 0L, 0L, RealtimeScope.USER_NOTIFICATIONS);
    private final RealtimePrincipal general = new RealtimePrincipal(7L, 3L, 0L, 0L);

    @AfterEach
    void clearTenant() { TenantContextHolder.clear(); }

    @Test
    void ordinaryMessagePermissionDoesNotGrantDiagnosticConnection() {
        when(permissions.getPermsByUserId(7L)).thenReturn(Set.of("system:message:list"));
        assertThat(policy.isAuthorized(notifications)).isTrue();
        assertThat(policy.isAuthorized(general)).isFalse();
        when(permissions.getPermsByUserId(7L)).thenReturn(Set.of());
        assertThat(policy.isAuthorized(notifications)).isFalse();
    }

    @Test
    void resolvesReceiverTenantAndRestoresPublisherContext() {
        TenantContextHolder.setTenantId(99L);
        when(permissions.getPermsByUserId(7L)).thenAnswer(call -> {
            assertThat(TenantContextHolder.getTenantIdOrNull()).isEqualTo(3L);
            return Set.of("system:realtime:view");
        });
        assertThat(policy.isAuthorized(general)).isTrue();
        assertThat(TenantContextHolder.getTenantIdOrNull()).isEqualTo(99L);
    }

    @Test
    void usesSameWildcardMatchingAsSaToken() {
        when(permissions.getPermsByUserId(7L)).thenReturn(Set.of("system:message:*"));
        assertThat(policy.isAuthorized(notifications)).isTrue();
        assertThat(policy.isAuthorized(general)).isFalse();
    }

    @Test
    void onlyTwoPersonalEventTypesReachNotificationConnections() {
        for (String type : Set.of("system-message", "system-message-state")) {
            RealtimeMessage message = RealtimeMessage.of(type, Map.of("id", 1));
            assertThat(policy.canReceive(notifications, RealtimeDeliveryKind.USER, message)).isTrue();
            assertThat(policy.canReceive(notifications, RealtimeDeliveryKind.TENANT, message)).isFalse();
            assertThat(policy.canReceive(notifications, RealtimeDeliveryKind.BROADCAST, message)).isFalse();
        }
        RealtimeMessage diagnostic = RealtimeMessage.of("test", Map.of("secret", "diagnostic"));
        assertThat(policy.canReceive(notifications, RealtimeDeliveryKind.USER, diagnostic)).isFalse();
        assertThat(policy.canReceive(general, RealtimeDeliveryKind.TENANT, diagnostic)).isTrue();
    }

    @Test
    void unavailablePermissionsFailClosedAndPreserveCause() {
        var cause = new DataAccessResourceFailureException("unavailable");
        when(permissions.getPermsByUserId(7L)).thenThrow(cause);
        assertThatThrownBy(() -> policy.isAuthorized(notifications))
                .isInstanceOfSatisfying(BusinessException.class, error -> assertThat(error.getCode()).isEqualTo(503))
                .hasCause(cause);
        assertThat(TenantContextHolder.getTenantIdOrNull()).isNull();
    }
}
