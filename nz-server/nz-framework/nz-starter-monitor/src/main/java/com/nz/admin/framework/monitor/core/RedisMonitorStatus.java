package com.nz.admin.framework.monitor.core;

import lombok.Data;

@Data
public class RedisMonitorStatus {
    private boolean ok;
    private String message;
    private String version;
    private String mode;
    private long connectedClients;
    private long usedMemoryBytes;
    private long keyCount;
}