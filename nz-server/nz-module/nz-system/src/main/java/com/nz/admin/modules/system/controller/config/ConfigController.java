package com.nz.admin.modules.system.controller.config;

import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.dataobject.config.ConfigDO;
import com.nz.admin.modules.system.service.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 系统参数控制器。
 */
@RestController
@RequestMapping("/api/system/config")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @SaCheckPermission("system:config:list")
    @GetMapping("/page")
    public R<PageResult<ConfigDO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String configName,
                                        @RequestParam(required = false) String configKey,
                                        @RequestParam(required = false) Integer status) {
        return R.ok(PageResult.of(configService.listPage(pageNum, pageSize, configName, configKey, status)));
    }

    @SaCheckPermission("system:config:query")
    @GetMapping("/{id}")
    public R<ConfigDO> getById(@PathVariable Long id) {
        return R.ok(configService.getById(id));
    }

    @Log(title = "系统参数", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:config:add")
    @PostMapping
    public R<Void> add(@RequestBody ConfigDO config) {
        configService.save(config);
        return R.ok();
    }

    @Log(title = "系统参数", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:config:edit")
    @PutMapping
    public R<Void> update(@RequestBody ConfigDO config) {
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
