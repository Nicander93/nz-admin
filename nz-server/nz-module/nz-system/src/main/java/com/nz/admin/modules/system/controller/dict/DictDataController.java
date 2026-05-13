package com.nz.admin.modules.system.controller.dict;

import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.common.core.R;
import com.nz.admin.modules.system.entity.dataobject.dict.DictDataDO;
import com.nz.admin.modules.system.service.dict.DictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/dict/data")
public class DictDataController {

    @Autowired
    private DictDataService dictDataService;

    @GetMapping("/type/{dictType}")
    public R<List<DictDataDO>> listByType(@PathVariable String dictType) {
        return R.ok(dictDataService.listByDictType(dictType));
    }

    @SaCheckPermission("system:dict:query")
    @GetMapping("/{id}")
    public R<DictDataDO> getById(@PathVariable Long id) {
        return R.ok(dictDataService.getById(id));
    }

    @SaCheckPermission("system:dict:add")
    @PostMapping
    public R<Void> add(@RequestBody DictDataDO dictData) {
        dictDataService.save(dictData);
        return R.ok();
    }

    @SaCheckPermission("system:dict:edit")
    @PutMapping
    public R<Void> update(@RequestBody DictDataDO dictData) {
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
