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
    @Test
    void invalidatingAnOldVersionKeepsNewlyLoggedInConnections() throws Exception {
        var version = new java.util.concurrent.atomic.AtomicLong(0);
        var registry = new RealtimeConnectionRegistry(new ObjectMapper().findAndRegisterModules(),
                Duration.ofMinutes(1), identity -> identity.userVersion() == version.get());
        var old = new RealtimePrincipal(7L, 3L, 0L, 0L);
        registry.connectSse(old);
        var oldSocket = mock(WebSocketSession.class);
        when(oldSocket.getId()).thenReturn("old");
        when(oldSocket.isOpen()).thenReturn(true);
        registry.connectWebSocket(oldSocket, old);
        version.incrementAndGet();
        var current = new RealtimePrincipal(7L, 3L, 1L, 0L);
        registry.connectSse(current);
        var newSocket = mock(WebSocketSession.class);
        when(newSocket.getId()).thenReturn("new");
        when(newSocket.isOpen()).thenReturn(true);
        registry.connectWebSocket(newSocket, current);

        registry.disconnectInvalidIdentities();

        assertThat(registry.stats()).isEqualTo(new RealtimeConnectionStats(1, 1));
        verify(oldSocket).close();
        org.mockito.Mockito.verify(newSocket, org.mockito.Mockito.never()).close();
        assertThat(registry.publishToUser(3L, 7L, RealtimeMessage.of("notice", "new only"))).isEqualTo(2);
        verify(oldSocket, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void storageFailureClosesConnectionsBeforeDelivering() throws Exception {
        var failed = new java.util.concurrent.atomic.AtomicBoolean();
        var registry = new RealtimeConnectionRegistry(new ObjectMapper().findAndRegisterModules(),
                Duration.ofMinutes(1), identity -> {
                    if (failed.get()) throw new IllegalStateException("database unavailable");
                    return true;
                });
        var socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn("failure");
        when(socket.isOpen()).thenReturn(true);
        registry.connectWebSocket(socket, new RealtimePrincipal(7L, 3L, 0L, 0L));
        failed.set(true);

        assertThat(registry.publishToUser(3L, 7L, RealtimeMessage.of("notice", "blocked"))).isZero();
        assertThat(registry.stats().webSocketConnections()).isZero();
        verify(socket).close();
        verify(socket, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void deliveryRechecksIdentityWithoutWaitingForTheSweep() {
        var valid = new java.util.concurrent.atomic.AtomicBoolean(true);
        var registry = new RealtimeConnectionRegistry(new ObjectMapper().findAndRegisterModules(),
                Duration.ofMinutes(1), identity -> valid.get());
        registry.connectSse(new RealtimePrincipal(7L, 3L, 0L, 0L));
        valid.set(false);
        assertThat(registry.publishToUser(3L, 7L, RealtimeMessage.of("notice", "blocked"))).isZero();
        assertThat(registry.stats().sseConnections()).isZero();
    }
}

