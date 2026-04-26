package com.nz.admin.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.modules.system.entity.SysLoginLog;
import com.nz.admin.modules.system.query.SysLoginLogQuery;
import com.nz.admin.modules.system.service.SysLoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/login/log")
public class SysLoginLogController {

    @Autowired
    private SysLoginLogService loginLogService;

    @SaCheckPermission("system:loginlog:list")
    @GetMapping("/page")
    public R<Page<SysLoginLog>> page(SysLoginLogQuery query) {
        return R.ok(loginLogService.listPage(query));
    }

    @SaCheckPermission("system:loginlog:query")
    @GetMapping("/{id}")
    public R<SysLoginLog> getById(@PathVariable Long id) {
        return R.ok(loginLogService.getById(id));
    }
}
