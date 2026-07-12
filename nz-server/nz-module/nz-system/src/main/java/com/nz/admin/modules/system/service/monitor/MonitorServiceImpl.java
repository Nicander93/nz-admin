package com.nz.admin.modules.system.service.monitor;

import com.nz.admin.framework.monitor.core.MonitorStatus;
import com.nz.admin.framework.monitor.core.MonitorStatusProvider;
import com.nz.admin.modules.system.entity.vo.monitor.MonitorSummaryVO;
import org.springframework.stereotype.Service;

@Service
public class MonitorServiceImpl implements MonitorService {

    private final MonitorStatusProvider monitorStatusProvider;

    public MonitorServiceImpl(MonitorStatusProvider monitorStatusProvider) {
        this.monitorStatusProvider = monitorStatusProvider;
    }

    @Override
    public MonitorSummaryVO buildSummary() {
        MonitorStatus status = monitorStatusProvider.getStatus();
        MonitorSummaryVO vo = new MonitorSummaryVO();
        vo.setHealthStatus(status.getHealthStatus());
        vo.setDatabaseOk(status.isDatabaseOk());
        vo.setDatabaseMessage(status.getDatabaseMessage());
        vo.setHeapUsedBytes(status.getHeapUsedBytes());
        vo.setHeapMaxBytes(status.getHeapMaxBytes());
        vo.setUptimeMs(status.getUptimeMs());
        vo.setAvailableProcessors(status.getAvailableProcessors());
        vo.setDiskTotalBytes(status.getDiskTotalBytes());
        vo.setDiskFreeBytes(status.getDiskFreeBytes());
        vo.setRedisAvailable(status.isRedisAvailable());
        vo.setRedisOk(status.isRedisOk());
        vo.setRedisMessage(status.getRedisMessage());
        vo.setRedisVersion(status.getRedisVersion());
        vo.setRedisMode(status.getRedisMode());
        vo.setRedisConnectedClients(status.getRedisConnectedClients());
        vo.setRedisUsedMemoryBytes(status.getRedisUsedMemoryBytes());
        vo.setRedisKeyCount(status.getRedisKeyCount());
        return vo;
    }
}