package com.nz.admin.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.po.SysConfigDO;
import com.nz.admin.modules.system.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 系统参数控制器。
 */
@RestController
@RequestMapping("/api/system/config")
public class SysConfigController {

    @Autowired
    private SysConfigService configService;

    @SaCheckPermission("system:config:list")
    @GetMapping("/page")
    public R<Page<SysConfigDO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                    @RequestParam(required = false) String configName,
                                    @RequestParam(required = false) String configKey,
                                    @RequestParam(required = false) Integer status) {
        return R.ok(configService.listPage(pageNum, pageSize, configName, configKey, status));
    }

    @SaCheckPermission("system:config:query")
    @GetMapping("/{id}")
    public R<SysConfigDO> getById(@PathVariable Long id) {
        return R.ok(configService.getById(id));
    }

    @Log(title = "系统参数", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:config:add")
    @PostMapping
    public R<Void> add(@RequestBody SysConfigDO config) {
        configService.save(config);
        return R.ok();
    }

    @Log(title = "系统参数", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:config:edit")
    @PutMapping
    public R<Void> update(@RequestBody SysConfigDO config) {
        configService.updateById(config);
        return R.ok();
    }

    @Log(title = "系统参数", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:config:remove")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        configService.removeById(id);
        return R.ok();
    }
}
