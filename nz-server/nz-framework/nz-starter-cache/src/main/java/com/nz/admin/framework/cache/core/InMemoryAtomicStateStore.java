package com.nz.admin.framework.cache.core;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** 单实例默认存储，按分钟清理不再访问的过期状态。 */
public class InMemoryAtomicStateStore implements AtomicStateStore {
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicLong nextCleanup = new AtomicLong();
    private final Clock clock;

    public InMemoryAtomicStateStore() {
        this(Clock.systemUTC());
    }

    public InMemoryAtomicStateStore(Clock clock) {
        this.clock = clock;
    }

    private long expiry(Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("状态 TTL 必须大于零");
        return clock.millis() + ttl.toMillis();
    }

    private Entry live(Entry entry) {
        return entry != null && entry.expiry() > clock.millis() ? entry : null;
    }

    private void cleanup() {
        long now = clock.millis(), next = nextCleanup.get();
        if (now >= next && nextCleanup.compareAndSet(next, now + 60_000))
            entries.entrySet().removeIf(entry -> entry.getValue().expiry() <= now);
    }

    public String get(String key) {
        cleanup();
        var entry = entries.computeIfPresent(key, (k, v) -> live(v));
        return entry == null ? null : entry.value();
    }

    public void put(String key, String value, Duration ttl) {
        cleanup();
        entries.put(key, new Entry(value, expiry(ttl)));
    }

    public boolean putIfAbsent(String key, String value, Duration ttl) {
        return compareAndSet(key, null, value, ttl);
    }

    public String take(String key) {
        var entry = live(entries.remove(key));
        return entry == null ? null : entry.value();
    }

    public boolean compareAndSet(String key, String expected, String value, Duration ttl) {
        cleanup();
        long expiry = expiry(ttl);
        var changed = new AtomicReference<>(false);
        entries.compute(
                key,
                (k, v) -> {
                    var current = live(v);
                    if (!Objects.equals(current == null ? null : current.value(), expected))
                        return current;
                    changed.set(true);
                    return new Entry(value, expiry);
                });
        return changed.get();
    }

    public boolean deleteIfValue(String key, String expected) {
        var changed = new AtomicReference<>(false);
        entries.computeIfPresent(
                key,
                (k, v) -> {
                    var current = live(v);
                    if (current != null && Objects.equals(current.value(), expected)) {
                        changed.set(true);
                        return null;
                    }
                    return current;
                });
        return changed.get();
    }

    public long increment(String key, Duration window) {
        cleanup();
        long expires = expiry(window);
        var result = new AtomicLong();
        entries.compute(
                key,
                (k, v) -> {
                    var current = live(v);
                    long count = current == null ? 1 : Long.parseLong(current.value()) + 1;
                    result.set(count);
                    return new Entry(
                            Long.toString(count), current == null ? expires : current.expiry());
                });
        return result.get();
    }

    private record Entry(String value, long expiry) {}
}
