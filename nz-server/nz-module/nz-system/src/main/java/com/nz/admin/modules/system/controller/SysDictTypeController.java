package com.nz.admin.modules.system.controller;

import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.R;
import com.nz.admin.modules.system.entity.po.SysDictTypeDO;
import com.nz.admin.modules.system.entity.query.SysDictTypeQuery;
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
    public R<Page<SysDictTypeDO>> page(SysDictTypeQuery query) {
        return R.ok(dictTypeService.listPage(query));
    }

    @SaCheckPermission("system:dict:query")
    @GetMapping("/{id}")
    public R<SysDictTypeDO> getById(@PathVariable Long id) {
        return R.ok(dictTypeService.getById(id));
    }

    @SaCheckPermission("system:dict:add")
    @PostMapping
    public R<Void> add(@RequestBody SysDictTypeDO dictType) {
        dictTypeService.save(dictType);
        return R.ok();
    }

    @SaCheckPermission("system:dict:edit")
    @PutMapping
    public R<Void> update(@RequestBody SysDictTypeDO dictType) {
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
