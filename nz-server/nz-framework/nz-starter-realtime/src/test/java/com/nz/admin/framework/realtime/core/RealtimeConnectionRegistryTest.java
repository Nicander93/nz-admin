package com.nz.admin.framework.realtime.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @Test
    void notificationScopeOnlyReceivesAllowedPersonalEventsAndIgnoresBroadcasts() throws Exception {
        var registry = new RealtimeConnectionRegistry(
                new ObjectMapper().findAndRegisterModules(),
                Duration.ofMinutes(1),
                null,
                notificationPolicy(new java.util.concurrent.atomic.AtomicBoolean(true)));
        var principal = new RealtimePrincipal(7L, 3L, 0L, 0L, RealtimeScope.USER_NOTIFICATIONS);
        registry.connectSse(principal);
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn("notification");
        when(socket.isOpen()).thenReturn(true);
        registry.connectWebSocket(socket, principal);

        assertThat(registry.publishToUser(3L, 7L, RealtimeMessage.of("system-message", "private")))
                .isEqualTo(2);
        assertThat(registry.publishToUser(3L, 7L, RealtimeMessage.of("test", "private test")))
                .isZero();
        assertThat(registry.publishToTenant(3L, RealtimeMessage.of("system-message", "tenant")))
                .isZero();
        assertThat(registry.broadcast(RealtimeMessage.of("system-message-state", "global")))
                .isZero();
        assertThat(registry.stats()).isEqualTo(new RealtimeConnectionStats(1, 1));
        verify(socket, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void deniedPermissionClosesExactScopeConnectionsOnDelivery() throws Exception {
        var allowed = new java.util.concurrent.atomic.AtomicBoolean(true);
        var registry = new RealtimeConnectionRegistry(
                new ObjectMapper().findAndRegisterModules(),
                Duration.ofMinutes(1),
                null,
                notificationPolicy(allowed));
        var principal = new RealtimePrincipal(7L, 3L, 0L, 0L, RealtimeScope.USER_NOTIFICATIONS);
        registry.connectSse(principal);
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn("revoked");
        when(socket.isOpen()).thenReturn(true);
        registry.connectWebSocket(socket, principal);
        allowed.set(false);

        assertThat(registry.publishToUser(3L, 7L, RealtimeMessage.of("system-message", "blocked")))
                .isZero();
        assertThat(registry.stats()).isEqualTo(new RealtimeConnectionStats(0, 0));
        verify(socket).close();
        verify(socket, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void scheduledCheckDisconnectsIdleConnectionsWhenPermissionIsRevoked() throws Exception {
        var allowed = new java.util.concurrent.atomic.AtomicBoolean(true);
        var registry = new RealtimeConnectionRegistry(
                new ObjectMapper().findAndRegisterModules(),
                Duration.ofMinutes(1),
                null,
                notificationPolicy(allowed));
        var principal = new RealtimePrincipal(7L, 3L, 0L, 0L, RealtimeScope.USER_NOTIFICATIONS);
        registry.connectSse(principal);
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn("idle-revoked");
        when(socket.isOpen()).thenReturn(true);
        registry.connectWebSocket(socket, principal);
        allowed.set(false);

        registry.disconnectInvalidIdentities();

        assertThat(registry.stats()).isEqualTo(new RealtimeConnectionStats(0, 0));
        verify(socket).close();
    }

    @Test
    void eventDeniedByPolicyDoesNotDisconnectNotificationConnection() throws Exception {
        var registry = new RealtimeConnectionRegistry(
                new ObjectMapper().findAndRegisterModules(),
                Duration.ofMinutes(1),
                null,
                notificationPolicy(new java.util.concurrent.atomic.AtomicBoolean(true)));
        var principal = new RealtimePrincipal(7L, 3L, 0L, 0L, RealtimeScope.USER_NOTIFICATIONS);
        registry.connectSse(principal);

        assertThat(registry.broadcast(RealtimeMessage.of("system-message", "not a user delivery")))
                .isZero();
        assertThat(registry.stats().sseConnections()).isEqualTo(1);
    }

    @Test
    void policyFailureDuringDeliveryClosesOnlyTheAffectedPrincipalScope() throws Exception {
        var registry = new RealtimeConnectionRegistry(
                new ObjectMapper().findAndRegisterModules(),
                Duration.ofMinutes(1),
                null,
                new RealtimeAccessPolicy() {
                    @Override
                    public boolean isAuthorized(RealtimePrincipal principal) {
                        return true;
                    }

                    @Override
                    public boolean canReceive(
                            RealtimePrincipal principal,
                            RealtimeDeliveryKind deliveryKind,
                            RealtimeMessage message) {
                        if (principal.scope() == RealtimeScope.USER_NOTIFICATIONS) {
                            throw new IllegalStateException("permission store unavailable");
                        }
                        return true;
                    }
                });
        var notificationPrincipal = new RealtimePrincipal(
                7L, 3L, 0L, 0L, RealtimeScope.USER_NOTIFICATIONS);
        var generalPrincipal = new RealtimePrincipal(7L, 3L, 0L, 0L, RealtimeScope.GENERAL);
        WebSocketSession notification = mock(WebSocketSession.class);
        when(notification.getId()).thenReturn("notification-policy-error");
        when(notification.isOpen()).thenReturn(true);
        registry.connectWebSocket(notification, notificationPrincipal);
        WebSocketSession general = mock(WebSocketSession.class);
        when(general.getId()).thenReturn("general-policy-error");
        when(general.isOpen()).thenReturn(true);
        registry.connectWebSocket(general, generalPrincipal);

        assertThat(registry.publishToUser(3L, 7L, RealtimeMessage.of("system-message", "hello")))
                .isEqualTo(1);
        assertThat(registry.stats()).isEqualTo(new RealtimeConnectionStats(0, 1));
        verify(notification).close();
        verify(general, never()).close();
        verify(notification, never()).sendMessage(any(TextMessage.class));
        verify(general, times(2)).sendMessage(any(TextMessage.class));
    }

    @Test
    void legacyRegistryConstructorsRejectNotificationScope() {
        var registry = new RealtimeConnectionRegistry(
                new ObjectMapper().findAndRegisterModules(), Duration.ofMinutes(1));
        var principal = new RealtimePrincipal(7L, 3L, 0L, 0L, RealtimeScope.USER_NOTIFICATIONS);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registry.connectSse(principal))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    private RealtimeAccessPolicy notificationPolicy(java.util.concurrent.atomic.AtomicBoolean allowed) {
        return new RealtimeAccessPolicy() {
            @Override
            public boolean isAuthorized(RealtimePrincipal principal) {
                return principal.scope() == RealtimeScope.GENERAL || allowed.get();
            }

            @Override
            public boolean canReceive(
                    RealtimePrincipal principal,
                    RealtimeDeliveryKind deliveryKind,
                    RealtimeMessage message) {
                if (principal.scope() == RealtimeScope.GENERAL) return true;
                return deliveryKind == RealtimeDeliveryKind.USER
                        && ("system-message".equals(message.type())
                        || "system-message-state".equals(message.type()));
            }
        };
    }

    @Test
    void delayedRevocationCheckDoesNotCloseFreshConnectionAfterPermissionRestoration() throws Exception {
        var replacing = new java.util.concurrent.atomic.AtomicBoolean();
        var registryHolder = new java.util.concurrent.atomic.AtomicReference<RealtimeConnectionRegistry>();
        var principal = new RealtimePrincipal(7L, 3L, 0L, 0L);
        var current = mock(WebSocketSession.class);
        when(current.getId()).thenReturn("restored");
        when(current.isOpen()).thenReturn(true);
        RealtimeAccessPolicy policy = new RealtimeAccessPolicy() {
            public boolean isAuthorized(RealtimePrincipal identity) {
                if (replacing.compareAndSet(true, false)) {
                    try { registryHolder.get().connectWebSocket(current, principal); }
                    catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
                    return false;
                }
                return true;
            }
            public boolean canReceive(RealtimePrincipal identity, RealtimeDeliveryKind kind, RealtimeMessage message) {
                return true;
            }
        };
        var registry = new RealtimeConnectionRegistry(new ObjectMapper().findAndRegisterModules(),
                Duration.ofMinutes(1), identity -> true, policy);
        registryHolder.set(registry);
        var old = mock(WebSocketSession.class);
        when(old.getId()).thenReturn("revoked");
        when(old.isOpen()).thenReturn(true);
        registry.connectWebSocket(old, principal);
        replacing.set(true);
        registry.disconnectInvalidIdentities();
        assertThat(registry.stats().webSocketConnections()).isEqualTo(1);
        verify(old).close();
        verify(current, never()).close();
    }

}

