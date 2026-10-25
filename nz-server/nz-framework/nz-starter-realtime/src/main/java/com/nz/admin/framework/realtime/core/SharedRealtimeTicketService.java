package com.nz.admin.framework.realtime.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nz.admin.common.core.identity.IdentityValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nz.admin.framework.cache.core.AtomicStateStore;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** 票据跨节点消费；撤销通过用户版本失效，不枚举 Redis 键。 */
public class SharedRealtimeTicketService implements RealtimeTicketService {
    private final AtomicStateStore states;
    private final ObjectMapper mapper;
    private final Duration ttl;
    private final IdentityValidator validator;

    public SharedRealtimeTicketService(AtomicStateStore states, ObjectMapper mapper, Duration ttl) {
        this(states, mapper, ttl, null);
    }

    public SharedRealtimeTicketService(AtomicStateStore states, ObjectMapper mapper, Duration ttl, IdentityValidator validator) {
        this.states = states;
        this.mapper = mapper;
        this.ttl = ttl;
        this.validator = validator;
    }

    private String owner(Long tenant, Long user) {
        return "realtime:epoch:" + tenant + ":" + user;
    }

    public String issue(RealtimePrincipal principal, RealtimeTransport transport) {
        if (validator != null && !validator.isValid(principal.identity()))
            throw new IllegalArgumentException("实时连接身份已失效");
        String name = owner(principal.tenantId(), principal.userId());
        String epoch = null;
        boolean refreshed = false;
        for (int attempt = 0; attempt < 64 && !refreshed; attempt++) {
            states.putIfAbsent(name, UUID.randomUUID().toString(), ttl.multipliedBy(2));
            epoch = states.get(name);
            refreshed =
                    epoch != null && states.compareAndSet(name, epoch, epoch, ttl.multipliedBy(2));
        }
        if (!refreshed) throw new IllegalStateException("用户票据版本更新冲突，请重试");
        String ticket = UUID.randomUUID().toString();
        try {
            states.put(
                    "realtime:ticket:" + ticket,
                    mapper.writeValueAsString(new Entry(principal, transport, epoch)),
                    ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("票据无法保存", e);
        }
        return ticket;
    }

    public Optional<RealtimePrincipal> consume(String ticket, RealtimeTransport transport) {
        if (ticket == null || ticket.isBlank()) return Optional.empty();
        String value = states.take("realtime:ticket:" + ticket);
        if (value == null) return Optional.empty();
        try {
            var entry = mapper.readValue(value, Entry.class);
            if (entry.transport() != transport
                    || entry.epoch() == null
                    || !Objects.equals(
                            entry.epoch(),
                            states.get(
                                    owner(
                                            entry.principal().tenantId(),
                                            entry.principal().userId())))) return Optional.empty();
            if (validator != null && !validator.isValid(entry.principal().identity())) return Optional.empty();
            return Optional.of(entry.principal());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("票据无法读取", e);
        }
    }

    public int revokeUser(Long tenantId, Long userId) {
        states.put(owner(tenantId, userId), UUID.randomUUID().toString(), ttl.multipliedBy(2));
        return 0; // 共享实现仅返回实际删除数量，失效票据随 TTL 清理。
    }

    public record Entry(RealtimePrincipal principal, RealtimeTransport transport, String epoch) {}
}
