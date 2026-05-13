package com.nz.admin.modules.system.controller.monitor;

import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.modules.system.entity.vo.monitor.MonitorSummaryVO;
import com.nz.admin.modules.system.service.monitor.MonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/monitor")
public class MonitorController {

    @Autowired
    private MonitorService monitorService;

    @SaCheckPermission("system:monitor:query")
    @GetMapping("/summary")
    public R<MonitorSummaryVO> summary() {
        return R.ok(monitorService.buildSummary());
    }
}
