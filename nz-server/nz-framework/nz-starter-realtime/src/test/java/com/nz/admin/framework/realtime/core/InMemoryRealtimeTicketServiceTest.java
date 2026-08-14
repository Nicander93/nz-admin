package com.nz.admin.framework.realtime.core;

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
}
