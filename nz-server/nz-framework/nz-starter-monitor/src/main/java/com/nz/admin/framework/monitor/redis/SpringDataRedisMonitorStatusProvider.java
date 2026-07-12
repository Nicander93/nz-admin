package com.nz.admin.framework.monitor.redis;

import com.nz.admin.framework.monitor.core.RedisMonitorStatus;
import com.nz.admin.framework.monitor.core.RedisMonitorStatusProvider;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Properties;

public class SpringDataRedisMonitorStatusProvider implements RedisMonitorStatusProvider {
    private final RedisConnectionFactory connectionFactory;

    public SpringDataRedisMonitorStatusProvider(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public RedisMonitorStatus getStatus() {
        RedisMonitorStatus status = new RedisMonitorStatus();
        try (RedisConnection connection = connectionFactory.getConnection()) {
            Properties info = connection.info();
            status.setOk(true);
            status.setMessage("ok");
            status.setVersion(value(info, "redis_version"));
            status.setMode(value(info, "redis_mode"));
            status.setConnectedClients(number(info, "connected_clients"));
            status.setUsedMemoryBytes(number(info, "used_memory"));
            Long dbSize = connection.dbSize();
            status.setKeyCount(dbSize == null ? 0L : dbSize);
        } catch (Exception e) {
            status.setOk(false);
            status.setMessage(e.getMessage() == null ? "connect failed" : e.getMessage());
        }
        return status;
    }

    private static String value(Properties properties, String key) {
        return properties == null ? null : properties.getProperty(key);
    }

    private static long number(Properties properties, String key) {
        String value = value(properties, key);
        if (value == null || value.isBlank()) return 0L;
        try { return Long.parseLong(value); } catch (NumberFormatException ignored) { return 0L; }
    }
}