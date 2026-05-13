package com.nz.admin.modules.system.entity.vo.workbench;

import com.nz.admin.modules.system.entity.dataobject.log.LoginLogDO;
import com.nz.admin.modules.system.entity.dataobject.log.OperLogDO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WorkbenchSnapshotVO {

    private List<LoginLogDO> recentLoginLogs = new ArrayList<>();
    private List<OperLogDO> recentOperLogs = new ArrayList<>();
}
