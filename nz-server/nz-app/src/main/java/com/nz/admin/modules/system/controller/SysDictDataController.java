package com.nz.admin.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nz.admin.common.R;
import com.nz.admin.modules.system.entity.SysDictData;
import com.nz.admin.modules.system.service.SysDictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/dict/data")
public class SysDictDataController {

    @Autowired
    private SysDictDataService dictDataService;

    @GetMapping("/type/{dictType}")
    public R<List<SysDictData>> listByType(@PathVariable String dictType) {
        return R.ok(dictDataService.listByDictType(dictType));
    }

    @SaCheckPermission("system:dict:query")
    @GetMapping("/{id}")
    public R<SysDictData> getById(@PathVariable Long id) {
        return R.ok(dictDataService.getById(id));
    }

    @SaCheckPermission("system:dict:add")
    @PostMapping
    public R<Void> add(@RequestBody SysDictData dictData) {
        dictDataService.save(dictData);
        return R.ok();
    }

    @SaCheckPermission("system:dict:edit")
    @PutMapping
    public R<Void> update(@RequestBody SysDictData dictData) {
        dictDataService.updateById(dictData);
        return R.ok();
    }

    @SaCheckPermission("system:dict:remove")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        dictDataService.removeById(id);
        return R.ok();
    }
}
