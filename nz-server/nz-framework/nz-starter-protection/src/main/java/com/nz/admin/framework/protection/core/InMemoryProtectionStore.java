package com.nz.admin.framework.protection.core;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本地保护存储。
 */
public class InMemoryProtectionStore {

    private final ConcurrentHashMap<String, Long> repeatSubmitStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CounterWindow> rateLimitStore = new ConcurrentHashMap<>();

    public boolean isRepeatSubmit(String key, int intervalSeconds) {
        long now = Instant.now().toEpochMilli();
        long expireAt = now + intervalSeconds * 1000L;
        Long previous = repeatSubmitStore.put(key, expireAt);
        if (previous == null || previous < now) {
            return false;
        }
        repeatSubmitStore.put(key, previous);
        return true;
    }

    public boolean tryAcquire(String key, int permits, int windowSeconds) {
        long now = Instant.now().toEpochMilli();
        long windowEnd = now + windowSeconds * 1000L;
        CounterWindow counterWindow = rateLimitStore.compute(key, (ignored, current) -> {
            if (current == null || current.windowEnd < now) {
                return new CounterWindow(new AtomicInteger(1), windowEnd);
            }
            current.counter.incrementAndGet();
            return current;
        });
        return counterWindow.counter.get() <= permits;
    }

    private record CounterWindow(AtomicInteger counter, long windowEnd) {
    }
}
