package com.nz.admin.framework.realtime.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRealtimeTicketServiceTest {

    private static final RealtimePrincipal PRINCIPAL = new RealtimePrincipal(7L, 3L);

    @Test
    void consumesTicketOnlyOnceForMatchingTransport() {
        var service = new InMemoryRealtimeTicketService(
                Duration.ofSeconds(30),
                Clock.systemUTC()
        );
        String ticket = service.issue(PRINCIPAL, RealtimeTransport.SSE);

        assertThat(service.consume(ticket, RealtimeTransport.WEBSOCKET)).isEmpty();
        assertThat(service.consume(ticket, RealtimeTransport.SSE)).isEmpty();

        String validTicket = service.issue(PRINCIPAL, RealtimeTransport.SSE);
        assertThat(service.consume(validTicket, RealtimeTransport.SSE)).contains(PRINCIPAL);
        assertThat(service.consume(validTicket, RealtimeTransport.SSE)).isEmpty();
    }

    @Test
    void rejectsExpiredAndBlankTickets() {
        var service = new InMemoryRealtimeTicketService(Duration.ZERO, Clock.systemUTC());
        String ticket = service.issue(PRINCIPAL, RealtimeTransport.SSE);

        assertThat(service.consume(ticket, RealtimeTransport.SSE)).isEmpty();
        assertThat(service.consume(" ", RealtimeTransport.SSE)).isEmpty();
    }

    @Test
    void revokesOnlyTicketsOwnedByTheTargetUserAndTenant() {
        var service = new InMemoryRealtimeTicketService(
                Duration.ofSeconds(30),
                Clock.systemUTC()
        );
        String revoked = service.issue(PRINCIPAL, RealtimeTransport.SSE);
        String retained = service.issue(
                new RealtimePrincipal(8L, 3L),
                RealtimeTransport.SSE
        );

        assertThat(service.revokeUser(3L, 7L)).isEqualTo(1);
        assertThat(service.consume(revoked, RealtimeTransport.SSE)).isEmpty();
        assertThat(service.consume(retained, RealtimeTransport.SSE)).isPresent();
    }
    @Test
    void roundTripsNotificationScopeAndDefaultsMissingLegacyScopeToGeneral() throws Exception {
        var notificationPolicy = new RealtimeAccessPolicy() {
            @Override
            public boolean isAuthorized(RealtimePrincipal principal) {
                return true;
            }

            @Override
            public boolean canReceive(
                    RealtimePrincipal principal,
                    RealtimeDeliveryKind deliveryKind,
                    RealtimeMessage message) {
                return true;
            }
        };
        var service = new InMemoryRealtimeTicketService(
                Duration.ofSeconds(30), Clock.systemUTC(), null, notificationPolicy);
        var principal = new RealtimePrincipal(7L, 3L, 1L, 2L, RealtimeScope.USER_NOTIFICATIONS);
        assertThat(service.consume(service.issue(principal, RealtimeTransport.SSE), RealtimeTransport.SSE))
                .contains(principal);

        var mapper = new ObjectMapper();
        var encoded = mapper.writeValueAsString(principal);
        assertThat(mapper.readValue(encoded, RealtimePrincipal.class)).isEqualTo(principal);
        var legacy = mapper.readValue("{\"userId\":7,\"tenantId\":3,\"userVersion\":1,\"tenantVersion\":2}", RealtimePrincipal.class);
        assertThat(legacy.scope()).isEqualTo(RealtimeScope.GENERAL);
        assertThat(RealtimeAccessPolicy.generalOnly().isAuthorized(legacy)).isTrue();
    }

    @Test
    void rechecksNotificationPermissionWhenConsumingTicket() {
        var allowed = new java.util.concurrent.atomic.AtomicBoolean(true);
        var policy = new RealtimeAccessPolicy() {
            @Override
            public boolean isAuthorized(RealtimePrincipal principal) {
                return allowed.get();
            }

            @Override
            public boolean canReceive(
                    RealtimePrincipal principal,
                    RealtimeDeliveryKind deliveryKind,
                    RealtimeMessage message) {
                return true;
            }
        };
        var service = new InMemoryRealtimeTicketService(
                Duration.ofSeconds(30), Clock.systemUTC(), null, policy);
        var principal = new RealtimePrincipal(7L, 3L, 1L, 2L, RealtimeScope.USER_NOTIFICATIONS);
        String ticket = service.issue(principal, RealtimeTransport.SSE);
        allowed.set(false);

        assertThat(service.consume(ticket, RealtimeTransport.SSE)).isEmpty();
    }

    @Test
    void legacyTicketConstructorsRejectNotificationScope() {
        var service = new InMemoryRealtimeTicketService(Duration.ofSeconds(30), Clock.systemUTC());
        var principal = new RealtimePrincipal(7L, 3L, 1L, 2L, RealtimeScope.USER_NOTIFICATIONS);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.issue(principal, RealtimeTransport.SSE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void propagatesAccessPolicyFailuresDuringIssueAndConsume() {
        var failed = new java.util.concurrent.atomic.AtomicBoolean();
        var policy = new RealtimeAccessPolicy() {
            @Override
            public boolean isAuthorized(RealtimePrincipal principal) {
                if (failed.get()) throw new IllegalStateException("policy unavailable");
                return true;
            }

            @Override
            public boolean canReceive(
                    RealtimePrincipal principal,
                    RealtimeDeliveryKind deliveryKind,
                    RealtimeMessage message) {
                return true;
            }
        };
        var service = new InMemoryRealtimeTicketService(
                Duration.ofSeconds(30), Clock.systemUTC(), null, policy);
        String ticket = service.issue(PRINCIPAL, RealtimeTransport.SSE);
        failed.set(true);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.issue(PRINCIPAL, RealtimeTransport.SSE))
                .isInstanceOf(IllegalStateException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.consume(ticket, RealtimeTransport.SSE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anInvalidatedTicketCannotReviveAfterReenablingTheIdentity() {
        var valid = new java.util.concurrent.atomic.AtomicBoolean(true);
        var service = new InMemoryRealtimeTicketService(Duration.ofSeconds(30), Clock.systemUTC(), identity -> valid.get());
        var principal = new RealtimePrincipal(7L, 3L, 0L, 0L);
        String old = service.issue(principal, RealtimeTransport.SSE);
        valid.set(false);
        assertThat(service.consume(old, RealtimeTransport.SSE)).isEmpty();
        valid.set(true);
        assertThat(service.consume(old, RealtimeTransport.SSE)).isEmpty();
        assertThat(service.consume(service.issue(principal, RealtimeTransport.SSE), RealtimeTransport.SSE)).contains(principal);
    }
}
