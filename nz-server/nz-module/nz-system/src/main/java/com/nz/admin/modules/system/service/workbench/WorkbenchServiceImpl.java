package com.nz.admin.modules.system.service.workbench;

import com.nz.admin.modules.system.entity.query.log.LoginLogQuery;
import com.nz.admin.modules.system.entity.query.log.OperLogQuery;
import com.nz.admin.modules.system.entity.vo.workbench.WorkbenchSnapshotVO;
import com.nz.admin.modules.system.service.log.LoginLogService;
import com.nz.admin.modules.system.service.log.OperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkbenchServiceImpl implements WorkbenchService {

    private static final int SNAPSHOT_PAGE_SIZE = 5;

    @Autowired
    private LoginLogService loginLogService;
    @Autowired
    private OperLogService operLogService;

    @Override
    public WorkbenchSnapshotVO buildSnapshot() {
        WorkbenchSnapshotVO vo = new WorkbenchSnapshotVO();

        LoginLogQuery loginQuery = new LoginLogQuery();
        loginQuery.setPageNum(1);
        loginQuery.setPageSize(SNAPSHOT_PAGE_SIZE);
        vo.getRecentLoginLogs().addAll(loginLogService.listPage(loginQuery).getRecords());

        OperLogQuery operQuery = new OperLogQuery();
        operQuery.setPageNum(1);
        operQuery.setPageSize(SNAPSHOT_PAGE_SIZE);
        vo.getRecentOperLogs().addAll(operLogService.listPage(operQuery).getRecords());

        return vo;
    }
}
