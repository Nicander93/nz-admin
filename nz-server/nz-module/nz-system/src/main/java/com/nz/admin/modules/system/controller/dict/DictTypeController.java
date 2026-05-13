package com.nz.admin.modules.system.controller.dict;

import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.modules.system.entity.dataobject.dict.DictTypeDO;
import com.nz.admin.modules.system.entity.query.dict.DictTypeQuery;
import com.nz.admin.modules.system.service.dict.DictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/dict/type")
public class DictTypeController {

    @Autowired
    private DictTypeService dictTypeService;

    @SaCheckPermission("system:dict:list")
    @GetMapping("/page")
    public R<PageResult<DictTypeDO>> page(DictTypeQuery query) {
        return R.ok(PageResult.of(dictTypeService.listPage(query)));
    }

    @SaCheckPermission("system:dict:query")
    @GetMapping("/{id}")
    public R<DictTypeDO> getById(@PathVariable Long id) {
        return R.ok(dictTypeService.getById(id));
    }

    @SaCheckPermission("system:dict:add")
    @PostMapping
    public R<Void> add(@RequestBody DictTypeDO dictType) {
        dictTypeService.save(dictType);
        return R.ok();
    }

    @SaCheckPermission("system:dict:edit")
    @PutMapping
    public R<Void> update(@RequestBody DictTypeDO dictType) {
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
