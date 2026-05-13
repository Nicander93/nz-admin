package com.nz.admin.modules.system.controller.workbench;

import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.modules.system.entity.vo.workbench.WorkbenchSnapshotVO;
import com.nz.admin.modules.system.service.workbench.WorkbenchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/workbench")
public class WorkbenchController {

    @Autowired
    private WorkbenchService workbenchService;

    @SaCheckPermission("system:workbench:view")
    @GetMapping("/snapshot")
    public R<WorkbenchSnapshotVO> snapshot() {
        return R.ok(workbenchService.buildSnapshot());
    }
}
