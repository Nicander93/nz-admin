package com.nz.admin.modules.system.controller;

import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.common.R;
import com.nz.admin.modules.system.entity.po.SysDeptDO;
import com.nz.admin.modules.system.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/dept")
public class SysDeptController {

    @Autowired
    private SysDeptService deptService;

    @SaCheckPermission("system:dept:list")
    @GetMapping("/list")
    public R<List<SysDeptDO>> list() {
        return R.ok(deptService.listAll());
    }

    @SaCheckPermission("system:dept:query")
    @GetMapping("/{id}")
    public R<SysDeptDO> getById(@PathVariable Long id) {
        return R.ok(deptService.getById(id));
    }

    @SaCheckPermission("system:dept:add")
    @PostMapping
    public R<Void> add(@RequestBody SysDeptDO dept) {
        deptService.save(dept);
        return R.ok();
    }

    @SaCheckPermission("system:dept:edit")
    @PutMapping
    public R<Void> update(@RequestBody SysDeptDO dept) {
        deptService.updateById(dept);
        return R.ok();
    }

    @SaCheckPermission("system:dept:remove")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        deptService.removeById(id);
        return R.ok();
    }
}
