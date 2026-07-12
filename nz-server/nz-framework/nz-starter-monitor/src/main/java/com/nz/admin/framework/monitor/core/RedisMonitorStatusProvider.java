package com.nz.admin.framework.monitor.core;

@FunctionalInterface
public interface RedisMonitorStatusProvider {
    RedisMonitorStatus getStatus();
}