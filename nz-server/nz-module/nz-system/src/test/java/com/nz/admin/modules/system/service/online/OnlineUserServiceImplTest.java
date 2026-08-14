package com.nz.admin.modules.system.service.online;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.tenant.config.TenantProperties;
import com.nz.admin.framework.realtime.core.RealtimeConnectionManager;
import com.nz.admin.framework.realtime.core.RealtimeTicketService;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnlineUserServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private OnlineSessionAccessor sessionAccessor;

    private OnlineUserServiceImpl service;

    @Mock
    private RealtimeConnectionManager realtimeConnectionManager;
    @Mock
    private RealtimeTicketService realtimeTicketService;

    @BeforeEach
    void setUpService() {
        TenantProperties tenantProperties = new TenantProperties();
        tenantProperties.setDefaultTenantId(1L);
        service = new OnlineUserServiceImpl(sessionAccessor, tenantProperties);
        ReflectionTestUtils.setField(service, "realtimeConnectionManager", realtimeConnectionManager);
        ReflectionTestUtils.setField(service, "realtimeTicketService", realtimeTicketService);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void defaultTenantCanSearchSessionsAcrossTenants() {
        TenantContextHolder.setTenantId(1L);
        when(sessionAccessor.listTokenValues()).thenReturn(List.of("token-a", "token-b"));
        when(sessionAccessor.getSnapshot("token-a")).thenReturn(snapshot("token-a", 1L, "admin", "10.0.0.1"));
        when(sessionAccessor.getSnapshot("token-b")).thenReturn(snapshot("token-b", 2L, "tenant-user", "10.0.0.2"));

        var result = service.listOnlineUsers("tenant", "10.0.0.2");

        assertThat(result).singleElement().satisfies(user -> {
            assertThat(user.getTokenValue()).isEqualTo("token-b");
            assertThat(user.getTenantId()).isEqualTo(2L);
            assertThat(user.getUsername()).isEqualTo("tenant-user");
        });
    }

    @Test
    void regularTenantOnlySeesItsOwnLiveSessions() {
        TenantContextHolder.setTenantId(2L);
        when(sessionAccessor.listTokenValues()).thenReturn(List.of("token-a", "token-b", "stale"));
        when(sessionAccessor.getSnapshot("token-a")).thenReturn(snapshot("token-a", 1L, "admin", "10.0.0.1"));
        when(sessionAccessor.getSnapshot("token-b")).thenReturn(snapshot("token-b", 2L, "tenant-user", "10.0.0.2"));
        when(sessionAccessor.getSnapshot("stale")).thenReturn(null);

        var result = service.listOnlineUsers(null, null);

        assertThat(result).extracting("tokenValue").containsExactly("token-b");
    }

    @Test
    void regularTenantCannotLogoutAnotherTenantSession() {
        TenantContextHolder.setTenantId(2L);
        when(sessionAccessor.getSnapshot("token-a")).thenReturn(snapshot("token-a", 1L, "admin", "10.0.0.1"));

        assertThatThrownBy(() -> service.forceLogout("token-a"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
        verify(sessionAccessor, never()).logout("token-a");
    }

    @Test
    void tenantCanLogoutItsOwnSession() {
        TenantContextHolder.setTenantId(2L);
        when(sessionAccessor.getSnapshot("token-b")).thenReturn(snapshot("token-b", 2L, "tenant-user", "10.0.0.2"));

        service.forceLogout("token-b");

        verify(sessionAccessor).logout("token-b");
        verify(realtimeTicketService).revokeUser(2L, 20L);
        verify(realtimeConnectionManager).disconnectUser(2L, 20L);
    }

    private OnlineSessionSnapshot snapshot(String tokenValue, Long tenantId, String username, String loginIp) {
        return new OnlineSessionSnapshot(
                tokenValue,
                tenantId * 10,
                tenantId,
                tenantId == 1L ? "default" : "tenant-" + tenantId,
                username,
                "研发部",
                loginIp,
                LocalDateTime.of(2026, 8, 11, 10, 0),
                "Mozilla/5.0",
                3600
        );
    }
}
