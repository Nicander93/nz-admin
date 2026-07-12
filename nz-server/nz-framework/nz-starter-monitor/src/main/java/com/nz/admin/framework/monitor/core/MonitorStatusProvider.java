package com.nz.admin.framework.monitor.core;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class MonitorStatusProvider {

    private final HealthEndpoint healthEndpoint;
    private final DataSource dataSource;
    private final RedisMonitorStatusProvider redisStatusProvider;

    public MonitorStatusProvider(HealthEndpoint healthEndpoint, DataSource dataSource,
                                 RedisMonitorStatusProvider redisStatusProvider) {
        this.healthEndpoint = healthEndpoint;
        this.dataSource = dataSource;
        this.redisStatusProvider = redisStatusProvider;
    }

    public MonitorStatus getStatus() {
        MonitorStatus status = new MonitorStatus();
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        status.setHeapUsedBytes(memoryMXBean.getHeapMemoryUsage().getUsed());
        status.setHeapMaxBytes(memoryMXBean.getHeapMemoryUsage().getMax());
        status.setUptimeMs(ManagementFactory.getRuntimeMXBean().getUptime());
        status.setAvailableProcessors(runtime.availableProcessors());
        File root = new File(".");
        status.setDiskTotalBytes(root.getTotalSpace());
        status.setDiskFreeBytes(root.getFreeSpace());

        if (redisStatusProvider != null) {
            RedisMonitorStatus redis = redisStatusProvider.getStatus();
            status.setRedisAvailable(true);
            status.setRedisOk(redis.isOk());
            status.setRedisMessage(redis.getMessage());
            status.setRedisVersion(redis.getVersion());
            status.setRedisMode(redis.getMode());
            status.setRedisConnectedClients(redis.getConnectedClients());
            status.setRedisUsedMemoryBytes(redis.getUsedMemoryBytes());
            status.setRedisKeyCount(redis.getKeyCount());
        }

        if (healthEndpoint != null) {
            HealthComponent health = healthEndpoint.health();
            status.setHealthStatus(health.getStatus().getCode());
        } else {
            status.setHealthStatus("UNKNOWN");
        }

        if (dataSource == null) {
            status.setDatabaseOk(false);
            status.setDatabaseMessage("dataSource unavailable");
            return status;
        }
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            status.setDatabaseOk(resultSet.next());
            status.setDatabaseMessage("ok");
        } catch (Exception e) {
            status.setDatabaseOk(false);
            status.setDatabaseMessage(e.getMessage() != null ? e.getMessage() : "connect failed");
        }
        return status;
    }
}