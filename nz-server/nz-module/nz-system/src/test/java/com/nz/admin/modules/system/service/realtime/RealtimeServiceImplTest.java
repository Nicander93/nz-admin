package com.nz.admin.modules.system.service.realtime;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.realtime.config.RealtimeProperties;
import com.nz.admin.framework.realtime.core.RealtimeConnectionStats;
import com.nz.admin.framework.realtime.core.RealtimePrincipal;
import com.nz.admin.framework.realtime.core.RealtimePublisher;
import com.nz.admin.framework.realtime.core.RealtimeTicketService;
import com.nz.admin.framework.realtime.core.RealtimeTransport;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeServiceImplTest {

    private final LoginUserContext loginUserContext = mock(LoginUserContext.class);
    private final RealtimeTicketService ticketService = mock(RealtimeTicketService.class);
    private final RealtimePublisher publisher = mock(RealtimePublisher.class);
    private final RealtimeProperties properties = new RealtimeProperties();
    private final RealtimeServiceImpl service =
            new RealtimeServiceImpl(loginUserContext, ticketService, publisher, properties);

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void issuesShortLivedTicketForCurrentTenantIdentity() {
        when(loginUserContext.getLoginUserIdOrNull()).thenReturn(7L);
        when(ticketService.issue(
                new RealtimePrincipal(7L, 3L),
                RealtimeTransport.WEBSOCKET
        )).thenReturn("ticket-1");
        properties.setTicketTtl(Duration.ofSeconds(20));
        TenantContextHolder.setTenantId(3L);

        var result = service.issueTicket(RealtimeTransport.WEBSOCKET);

        assertThat(result.getTicket()).isEqualTo("ticket-1");
        assertThat(result.getPath()).isEqualTo("/realtime/ws");
        assertThat(result.getExpiresInSeconds()).isEqualTo(20);
    }

    @Test
    void sendsTestMessageOnlyToCurrentUserAndTenant() {
        when(loginUserContext.getLoginUserIdOrNull()).thenReturn(7L);
        when(publisher.publishToUser(any(), any(), any())).thenReturn(2);
        TenantContextHolder.setTenantId(3L);

        assertThat(service.sendTestMessage("hello")).isEqualTo(2);
        verify(publisher).publishToUser(
                org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq(7L),
                any()
        );
    }

    @Test
    void rejectsMissingTenantContext() {
        when(loginUserContext.getLoginUserIdOrNull()).thenReturn(7L);

        assertThatThrownBy(() -> service.issueTicket(RealtimeTransport.SSE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("租户上下文");
    }

    @Test
    void returnsCurrentNodeStats() {
        var stats = new RealtimeConnectionStats(2, 1);
        when(publisher.stats()).thenReturn(stats);

        assertThat(service.getStats()).isEqualTo(stats);
    }
}
