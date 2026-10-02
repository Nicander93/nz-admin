package com.nz.admin.modules.system.controller.file;

import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.dto.file.FileConfigSaveRequest;
import com.nz.admin.modules.system.entity.vo.file.FileConfigVO;
import com.nz.admin.modules.system.service.file.FileConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 文件存储配置管理接口。 */
@RestController
@RequestMapping("/api/system/file-config")
public class FileConfigController {

    private final FileConfigService fileConfigService;

    public FileConfigController(FileConfigService fileConfigService) {
        this.fileConfigService = fileConfigService;
    }

    @SaCheckPermission("system:fileconfig:list")
    @GetMapping("/page")
    public R<PageResult<FileConfigVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) String configName,
                                            @RequestParam(required = false) String storageType,
                                            @RequestParam(required = false) Integer status) {
        return R.ok(PageResult.of(fileConfigService.page(pageNum, pageSize, configName, storageType, status)));
    }

    @SaCheckPermission("system:fileconfig:query")
    @GetMapping("/{id}")
    public R<FileConfigVO> get(@PathVariable Long id) {
        return R.ok(fileConfigService.get(id));
    }

    @Log(title = "文件配置", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:fileconfig:add")
    @PostMapping
    public R<Long> create(@Valid @RequestBody FileConfigSaveRequest request) {
        return R.ok(fileConfigService.create(request));
    }

    @Log(title = "文件配置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:fileconfig:edit")
    @PutMapping
    public R<Void> update(@Valid @RequestBody FileConfigSaveRequest request) {
        fileConfigService.update(request);
        return R.ok();
    }

    @Log(title = "文件配置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:fileconfig:edit")
    @PutMapping("/{id}/activate")
    public R<Void> activate(@PathVariable Long id) {
        fileConfigService.activate(id);
        return R.ok();
    }

    @Log(title = "文件配置连接测试", businessType = BusinessType.OTHER)
    @SaCheckPermission("system:fileconfig:test")
    @PostMapping("/{id}/test")
    public R<Void> testConnection(@PathVariable Long id) {
        fileConfigService.testConnection(id);
        return R.ok();
    }

    @Log(title = "文件配置", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:fileconfig:remove")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        fileConfigService.delete(id);
        return R.ok();
    }
}
