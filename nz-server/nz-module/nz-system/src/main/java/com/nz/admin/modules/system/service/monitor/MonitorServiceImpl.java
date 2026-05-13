package com.nz.admin.modules.system.service.monitor;

import com.nz.admin.framework.monitor.core.MonitorStatus;
import com.nz.admin.framework.monitor.core.MonitorStatusProvider;
import com.nz.admin.modules.system.entity.vo.monitor.MonitorSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MonitorServiceImpl implements MonitorService {

    @Autowired
    private MonitorStatusProvider monitorStatusProvider;

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
        return vo;
    }
}
