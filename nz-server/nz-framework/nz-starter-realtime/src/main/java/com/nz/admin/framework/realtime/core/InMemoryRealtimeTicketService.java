package com.nz.admin.framework.realtime.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单节点一次性票据实现。票据消费后立即删除，避免重放。
 */
public class InMemoryRealtimeTicketService implements RealtimeTicketService {

    private final Map<String, TicketEntry> tickets = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    public InMemoryRealtimeTicketService(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public String issue(RealtimePrincipal principal, RealtimeTransport transport) {
        Instant now = clock.instant();
        tickets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        String ticket = UUID.randomUUID().toString().replace("-", "");
        tickets.put(ticket, new TicketEntry(principal, transport, now.plus(ttl)));
        return ticket;
    }

    @Override
    public Optional<RealtimePrincipal> consume(String ticket, RealtimeTransport transport) {
        if (ticket == null || ticket.isBlank()) {
            return Optional.empty();
        }
        TicketEntry entry = tickets.remove(ticket);
        if (entry == null
                || entry.transport() != transport
                || !entry.expiresAt().isAfter(clock.instant())) {
            return Optional.empty();
        }
        return Optional.of(entry.principal());
    }

    @Override
    public int revokeUser(Long tenantId, Long userId) {
        int before = tickets.size();
        tickets.entrySet().removeIf(entry ->
                Objects.equals(entry.getValue().principal().tenantId(), tenantId)
                        && Objects.equals(entry.getValue().principal().userId(), userId));
        return before - tickets.size();
    }

    private record TicketEntry(
            RealtimePrincipal principal,
            RealtimeTransport transport,
            Instant expiresAt) {
    }
}
