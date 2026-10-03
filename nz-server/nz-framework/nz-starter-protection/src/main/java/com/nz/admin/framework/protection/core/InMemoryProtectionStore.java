package com.nz.admin.framework.protection.core;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** 单实例保护存储，操作原子执行，并按分钟清理过期键。 */
public class InMemoryProtectionStore implements ProtectionStore {
    private final ConcurrentHashMap<String, Long> repeats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Window> limits = new ConcurrentHashMap<>();
    private final Clock clock;
    private final AtomicLong nextCleanup = new AtomicLong();

    public InMemoryProtectionStore() { this(Clock.systemUTC()); }
    public InMemoryProtectionStore(Clock clock) { this.clock = clock; }

    @Override
    public boolean isRepeatSubmit(String key, int seconds) {
        if (seconds <= 0) throw new IllegalArgumentException("防重间隔必须大于零");
        long now = clock.millis();
        cleanup(now);
        AtomicBoolean repeat = new AtomicBoolean();
        repeats.compute(key, (ignored, expiry) -> {
            if (expiry != null && expiry > now) {
                repeat.set(true);
                return expiry;
            }
            return now + seconds * 1000L;
        });
        return repeat.get();
    }

    @Override
    public boolean tryAcquire(String key, int permits, int seconds) {
        if (permits <= 0 || seconds <= 0) throw new IllegalArgumentException("限流参数必须大于零");
        long now = clock.millis();
        cleanup(now);
        AtomicBoolean allowed = new AtomicBoolean();
        limits.compute(key, (ignored, current) -> {
            Window window = current == null || current.expiry() <= now
                    ? new Window(0, now + seconds * 1000L) : current;
            allowed.set(window.count() < permits);
            return new Window(Math.min(window.count() + 1, permits), window.expiry());
        });
        return allowed.get();
    }

    private void cleanup(long now) {
        long next = nextCleanup.get();
        if (now >= next && nextCleanup.compareAndSet(next, now + 60_000)) {
            repeats.entrySet().removeIf(entry -> entry.getValue() <= now);
            limits.entrySet().removeIf(entry -> entry.getValue().expiry() <= now);
        }
    }
    private record Window(int count, long expiry) {}
}
