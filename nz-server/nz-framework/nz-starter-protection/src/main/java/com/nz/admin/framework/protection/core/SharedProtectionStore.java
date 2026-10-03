package com.nz.admin.framework.protection.core;

import com.nz.admin.framework.cache.core.AtomicStateStore;

import java.time.Duration;

/** 共享 TTL 防重和固定窗口限流。 */
public class SharedProtectionStore implements ProtectionStore {
    private final AtomicStateStore states;

    public SharedProtectionStore(AtomicStateStore states) {
        this.states = states;
    }

    public boolean isRepeatSubmit(String key, int seconds) {
        return !states.putIfAbsent(key, "1", Duration.ofSeconds(seconds));
    }

    public boolean tryAcquire(String key, int permits, int seconds) {
        if (permits <= 0) throw new IllegalArgumentException("限流次数必须大于零");
        return states.increment(key, Duration.ofSeconds(seconds)) <= permits;
    }
}
