package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.log.LoginLogDO;
import com.nz.admin.modules.system.entity.dataobject.log.OperLogDO;
import com.nz.admin.modules.system.entity.query.log.LoginLogQuery;
import com.nz.admin.modules.system.entity.query.log.OperLogQuery;
import com.nz.admin.modules.system.entity.vo.workbench.WorkbenchSnapshotVO;
import com.nz.admin.modules.system.service.log.LoginLogService;
import com.nz.admin.modules.system.service.log.OperLogService;
import com.nz.admin.modules.system.service.workbench.WorkbenchServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkbenchServiceImplTest {

    @Mock
    private LoginLogService loginLogService;
    @Mock
    private OperLogService operLogService;

    @InjectMocks
    private WorkbenchServiceImpl workbenchService;

    @Test
    void buildSnapshotMergesRecentLogs() {
        LoginLogDO login = new LoginLogDO().setId(1L).setUsername("a");
        Page<LoginLogDO> loginPage = new Page<>(1, 5);
        loginPage.setRecords(List.of(login));
        when(loginLogService.listPage(any(LoginLogQuery.class))).thenReturn(loginPage);

        OperLogDO oper = new OperLogDO().setId(2L).setTitle("t");
        Page<OperLogDO> operPage = new Page<>(1, 5);
        operPage.setRecords(List.of(oper));
        when(operLogService.listPage(any(OperLogQuery.class))).thenReturn(operPage);

        WorkbenchSnapshotVO snap = workbenchService.buildSnapshot();
        assertEquals(1, snap.getRecentLoginLogs().size());
        assertEquals(1, snap.getRecentOperLogs().size());
        assertEquals("a", snap.getRecentLoginLogs().get(0).getUsername());
        assertEquals("t", snap.getRecentOperLogs().get(0).getTitle());
    }
}
