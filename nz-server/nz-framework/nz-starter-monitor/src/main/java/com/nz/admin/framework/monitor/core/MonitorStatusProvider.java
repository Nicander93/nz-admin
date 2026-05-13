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

/**
 * 运行状态提供者。
 */
public class MonitorStatusProvider {

    private final HealthEndpoint healthEndpoint;
    private final DataSource dataSource;

    public MonitorStatusProvider(HealthEndpoint healthEndpoint, DataSource dataSource) {
        this.healthEndpoint = healthEndpoint;
        this.dataSource = dataSource;
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
