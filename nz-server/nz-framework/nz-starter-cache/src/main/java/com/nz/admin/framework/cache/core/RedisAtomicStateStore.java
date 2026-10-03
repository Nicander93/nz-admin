package com.nz.admin.framework.cache.core;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

/** 所有条件更新通过 Lua 原子执行，Redis 故障直接传播。 */
public class RedisAtomicStateStore implements AtomicStateStore {
    private final StringRedisTemplate redis;
    private final String prefix;

    public RedisAtomicStateStore(StringRedisTemplate redis, String prefix) {
        this.redis = redis;
        this.prefix = prefix + ":state:";
    }

    private String key(String key) {
        return prefix + key;
    }

    private long ttl(Duration duration) {
        if (duration.toMillis() <= 0) throw new IllegalArgumentException("状态 TTL 必须大于零");
        return duration.toMillis();
    }

    public String get(String key) {
        return redis.opsForValue().get(key(key));
    }

    public void put(String key, String value, Duration ttl) {
        redis.opsForValue().set(key(key), value, Duration.ofMillis(ttl(ttl)));
    }

    public boolean putIfAbsent(String key, String value, Duration ttl) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(key(key), value, Duration.ofMillis(ttl(ttl))));
    }

    public String take(String key) {
        return redis.opsForValue().getAndDelete(key(key));
    }

    public boolean compareAndSet(String key, String expected, String value, Duration ttl) {
        String script =
                "local v=redis.call('get',KEYS[1]); if (ARGV[1]=='0' and not v) or (ARGV[1]=='1'"
                    + " and v==ARGV[2]) then redis.call('psetex',KEYS[1],ARGV[4],ARGV[3]); return 1"
                    + " end; return 0";
        return Long.valueOf(1)
                .equals(
                        redis.execute(
                                new DefaultRedisScript<>(script, Long.class),
                                List.of(key(key)),
                                expected == null ? "0" : "1",
                                expected == null ? "" : expected,
                                value,
                                Long.toString(ttl(ttl))));
    }

    public boolean deleteIfValue(String key, String expected) {
        String script =
                "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) end;"
                    + " return 0";
        return Long.valueOf(1)
                .equals(
                        redis.execute(
                                new DefaultRedisScript<>(script, Long.class),
                                List.of(key(key)),
                                expected));
    }

    public long increment(String key, Duration window) {
        String script =
                "local n=redis.call('incr',KEYS[1]); if n==1 then"
                    + " redis.call('pexpire',KEYS[1],ARGV[1]) end; return n";
        Long value =
                redis.execute(
                        new DefaultRedisScript<>(script, Long.class),
                        List.of(key(key)),
                        Long.toString(ttl(window)));
        if (value == null) throw new IllegalStateException("Redis 未返回计数结果");
        return value;
    }
}
