package com.nz.admin.framework.realtime.core;

import com.nz.admin.common.core.identity.IdentityValidator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单节点一次性票据实现。票据消费后立即删除，避免重放。
 */
public class InMemoryRealtimeTicketService implements RealtimeTicketService {

    private final Map<String, TicketEntry> tickets = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;
    private final IdentityValidator validator;
    private final RealtimeAccessPolicy accessPolicy;

    public InMemoryRealtimeTicketService(Duration ttl, Clock clock) {
        this(ttl, clock, null, RealtimeAccessPolicy.generalOnly());
    }

    public InMemoryRealtimeTicketService(Duration ttl, Clock clock, IdentityValidator validator) {
        this(ttl, clock, validator, RealtimeAccessPolicy.generalOnly());
    }

    public InMemoryRealtimeTicketService(
            Duration ttl,
            Clock clock,
            IdentityValidator validator,
            RealtimeAccessPolicy accessPolicy) {
        this.ttl = ttl;
        this.clock = clock;
        this.validator = validator;
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy");
    }

    @Override
    public String issue(RealtimePrincipal principal, RealtimeTransport transport) {
        if (!isAuthorized(principal)) {
            throw new IllegalArgumentException("实时连接身份或权限已失效");
        }
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
        if (!isAuthorized(entry.principal())) return Optional.empty();
        return Optional.of(entry.principal());
    }

    private boolean isAuthorized(RealtimePrincipal principal) {
        return principal != null
                && (validator == null || validator.isValid(principal.identity()))
                && accessPolicy.isAuthorized(principal);
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
