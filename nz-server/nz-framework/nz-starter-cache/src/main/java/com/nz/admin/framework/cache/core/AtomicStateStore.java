package com.nz.admin.framework.cache.core;

import java.time.Duration;

/** 带过期时间的原子状态端口，用于一次性状态、防重和有界计数。 */
public interface AtomicStateStore {
    String get(String key);

    void put(String key, String value, Duration ttl);

    boolean putIfAbsent(String key, String value, Duration ttl);

    String take(String key);

    boolean compareAndSet(String key, String expected, String value, Duration ttl);

    boolean deleteIfValue(String key, String expected);

    long increment(String key, Duration window);
}
