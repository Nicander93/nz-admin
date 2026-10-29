package com.nz.admin.framework.quartz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

/** 调度实例和持久化工作队列的部署参数。 */
@ConfigurationProperties(prefix = "nz.job")
public class JobSchedulerProperties {
    private boolean clusterEnabled;
    private String schedulerName = "nz-admin";
    private int threadCount = 4;
    private Duration clusterCheckinInterval = Duration.ofSeconds(5);
    private Duration reconcileInterval = Duration.ofSeconds(30);
    private Duration executionLease = Duration.ofSeconds(60);
    private int maxDispatchAttempts = 6;

    public boolean isClusterEnabled() { return clusterEnabled; }
    public void setClusterEnabled(boolean value) { clusterEnabled = value; }
    public String getSchedulerName() { return schedulerName; }
    public void setSchedulerName(String value) { schedulerName = value; }
    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int value) { threadCount = value; }
    public Duration getClusterCheckinInterval() { return clusterCheckinInterval; }
    public void setClusterCheckinInterval(Duration value) { clusterCheckinInterval = value; }
    public Duration getReconcileInterval() { return reconcileInterval; }
    public void setReconcileInterval(Duration value) { reconcileInterval = value; }
    public Duration getExecutionLease() { return executionLease; }
    public void setExecutionLease(Duration value) { executionLease = value; }
    public int getMaxDispatchAttempts() { return maxDispatchAttempts; }
    public void setMaxDispatchAttempts(int value) { maxDispatchAttempts = value; }
}
