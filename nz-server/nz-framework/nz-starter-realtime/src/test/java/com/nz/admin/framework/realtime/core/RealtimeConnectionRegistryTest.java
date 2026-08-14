package com.nz.admin.framework.realtime.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeConnectionRegistryTest {

    @Test
    void publishesOnlyToMatchingUserAcrossBothTransports() throws Exception {
        var registry = new RealtimeConnectionRegistry(
                new ObjectMapper().findAndRegisterModules(),
                Duration.ofMinutes(1)
        );
        RealtimePrincipal principal = new RealtimePrincipal(7L, 3L);
        registry.connectSse(principal);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("ws-1");
        when(session.isOpen()).thenReturn(true);
        registry.connectWebSocket(session, principal);

        int delivered = registry.publishToUser(
                3L,
                7L,
                RealtimeMessage.of("notice", "hello")
        );

        assertThat(delivered).isEqualTo(2);
        assertThat(registry.publishToUser(
                4L,
                7L,
                RealtimeMessage.of("notice", "hidden")
        )).isZero();
        assertThat(registry.stats()).isEqualTo(new RealtimeConnectionStats(1, 1));
        verify(session, times(2)).sendMessage(any(TextMessage.class));
    }

    @Test
    void removesClosedWebSocketConnections() throws Exception {
        var registry = new RealtimeConnectionRegistry(
                new ObjectMapper().findAndRegisterModules(),
                Duration.ofMinutes(1)
        );
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("ws-2");
        when(session.isOpen()).thenReturn(true);
        registry.connectWebSocket(session, new RealtimePrincipal(8L, 3L));

        registry.disconnectWebSocket("ws-2");

        assertThat(registry.stats().webSocketConnections()).isZero();
    }
    @Test
    void disconnectsOnlyMatchingUserConnections() throws Exception {
        var registry = new RealtimeConnectionRegistry(
                new ObjectMapper().findAndRegisterModules(),
                Duration.ofMinutes(1)
        );
        RealtimePrincipal target = new RealtimePrincipal(7L, 3L);
        registry.connectSse(target);
        registry.connectSse(new RealtimePrincipal(8L, 3L));

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("ws-target");
        when(session.isOpen()).thenReturn(true);
        registry.connectWebSocket(session, target);

        assertThat(registry.disconnectUser(3L, 7L)).isEqualTo(2);
        assertThat(registry.stats()).isEqualTo(new RealtimeConnectionStats(1, 0));
        verify(session).close();
    }
}

