package com.nz.admin.modules.system.entity.vo.monitor;

import lombok.Data;

@Data
public class MonitorSummaryVO {

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
