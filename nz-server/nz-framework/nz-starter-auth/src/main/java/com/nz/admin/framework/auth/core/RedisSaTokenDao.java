package com.nz.admin.framework.auth.core;

import cn.dev33.satoken.dao.auto.SaTokenDaoBySessionFollowObject;

import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** 集群会话 DAO；保留 Sa-Token 的过期语义，在线查询使用 SCAN。 */
public class RedisSaTokenDao implements SaTokenDaoBySessionFollowObject {
    private final StringRedisTemplate strings;
    private final RedisTemplate<String, Object> objects;
    private final String namespace;

    public RedisSaTokenDao(StringRedisTemplate strings, String namespace) {
        this.strings = strings;
        this.namespace = namespace + ":auth:";
        objects = new RedisTemplate<>();
        objects.setConnectionFactory(strings.getConnectionFactory());
        objects.setKeySerializer(new StringRedisSerializer());
        var serializer = new GenericJackson2JsonRedisSerializer();
        serializer.configure(mapper -> mapper.findAndRegisterModules());
        objects.setValueSerializer(serializer);
        objects.afterPropertiesSet();
    }

    private String key(String key) {
        return namespace + key;
    }

    public String get(String key) {
        return strings.opsForValue().get(key(key));
    }

    public void set(String key, String value, long timeout) {
        if (timeout == NEVER_EXPIRE) strings.opsForValue().set(key(key), value);
        else if (timeout > 0)
            strings.opsForValue().set(key(key), value, Duration.ofSeconds(timeout));
    }

    public void update(String key, String value) {
        strings.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                        "if redis.call('exists',KEYS[1])==1 then"
                            + " redis.call('set',KEYS[1],ARGV[1],'KEEPTTL'); return 1 end; return"
                            + " 0",
                        Long.class),
                List.of(key(key)),
                value);
    }

    public void delete(String key) {
        strings.delete(key(key));
    }

    public long getTimeout(String key) {
        Long ttl = strings.getExpire(key(key));
        return ttl == null ? NOT_VALUE_EXPIRE : ttl;
    }

    public void updateTimeout(String key, long timeout) {
        if (timeout == NEVER_EXPIRE) strings.persist(key(key));
        else if (timeout > 0) strings.expire(key(key), Duration.ofSeconds(timeout));
        else delete(key);
    }

    public Object getObject(String key) {
        return objects.opsForValue().get(key(key));
    }

    public <T> T getObject(String key, Class<T> type) {
        return type.cast(getObject(key));
    }

    public void setObject(String key, Object value, long timeout) {
        if (timeout == NEVER_EXPIRE) objects.opsForValue().set(key(key), value);
        else if (timeout > 0)
            objects.opsForValue().set(key(key), value, Duration.ofSeconds(timeout));
    }

    public void updateObject(String key, Object value) {
        objects.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                        "if redis.call('exists',KEYS[1])==1 then"
                            + " redis.call('set',KEYS[1],ARGV[1],'KEEPTTL'); return 1 end; return"
                            + " 0",
                        Long.class),
                List.of(key(key)),
                value);
    }

    public void deleteObject(String key) {
        delete(key);
    }

    public long getObjectTimeout(String key) {
        return getTimeout(key);
    }

    public void updateObjectTimeout(String key, long timeout) {
        updateTimeout(key, timeout);
    }

    public List<String> searchData(
            String prefix, String keyword, int start, int size, boolean ascending) {
        List<String> found =
                strings.execute(
                        (RedisCallback<List<String>>)
                                connection -> {
                                    var result = new ArrayList<String>();
                                    try (var cursor =
                                            connection.scan(
                                                    ScanOptions.scanOptions()
                                                            .match(namespace + "*")
                                                            .count(500)
                                                            .build())) {
                                        while (cursor.hasNext()) {
                                            String name =
                                                    new String(
                                                                    cursor.next(),
                                                                    StandardCharsets.UTF_8)
                                                            .substring(namespace.length());
                                            if (name.startsWith(prefix) && name.contains(keyword))
                                                result.add(name);
                                        }
                                    }
                                    return result;
                                });
        if (found == null) return List.of();
        found.sort(ascending ? Comparator.naturalOrder() : Comparator.reverseOrder());
        int from = Math.max(0, start),
                to = size < 0 ? found.size() : Math.min(found.size(), from + size);
        return from >= found.size() ? List.of() : new ArrayList<>(found.subList(from, to));
    }
}
