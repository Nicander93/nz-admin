package com.nz.admin.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.R;
import com.nz.admin.modules.system.entity.SysDictType;
import com.nz.admin.modules.system.query.SysDictTypeQuery;
import com.nz.admin.modules.system.service.SysDictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/dict/type")
public class SysDictTypeController {

    @Autowired
    private SysDictTypeService dictTypeService;

    @SaCheckPermission("system:dict:list")
    @GetMapping("/page")
    public R<Page<SysDictType>> page(SysDictTypeQuery query) {
        return R.ok(dictTypeService.listPage(query));
    }

    @SaCheckPermission("system:dict:query")
    @GetMapping("/{id}")
    public R<SysDictType> getById(@PathVariable Long id) {
        return R.ok(dictTypeService.getById(id));
    }

    @SaCheckPermission("system:dict:add")
    @PostMapping
    public R<Void> add(@RequestBody SysDictType dictType) {
        dictTypeService.save(dictType);
        return R.ok();
    }

    @SaCheckPermission("system:dict:edit")
    @PutMapping
    public R<Void> update(@RequestBody SysDictType dictType) {
        dictTypeService.updateById(dictType);
        return R.ok();
    }

    @SaCheckPermission("system:dict:remove")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        dictTypeService.removeById(id);
        return R.ok();
    }
}
