package com.nz.admin.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.SysOperLog;
import com.nz.admin.modules.system.query.SysOperLogQuery;
import com.nz.admin.modules.system.service.SysOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/oper/log")
public class SysOperLogController {

    @Autowired
    private SysOperLogService operLogService;

    @SaCheckPermission("system:operlog:list")
    @GetMapping("/page")
    public R<Page<SysOperLog>> page(SysOperLogQuery query) {
        return R.ok(operLogService.listPage(query));
    }

    @SaCheckPermission("system:operlog:query")
    @GetMapping("/{id}")
    public R<SysOperLog> getById(@PathVariable Long id) {
        return R.ok(operLogService.getById(id));
    }

    @Log(title = "操作日志", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:operlog:remove")
    @DeleteMapping
    public R<Void> delete(@RequestBody List<Long> ids) {
        operLogService.removeByIds(ids);
        return R.ok();
    }
}
