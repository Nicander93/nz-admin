package com.nz.admin.framework.monitor.core;

import lombok.Data;

/**
 * 轻量运行状态对象。
 */
@Data
public class MonitorStatus {

    private String healthStatus;
    private boolean databaseOk;
    private String databaseMessage;
    private long heapUsedBytes;
    private long heapMaxBytes;
    private long uptimeMs;
    private int availableProcessors;
    private long diskTotalBytes;
    private long diskFreeBytes;
}
