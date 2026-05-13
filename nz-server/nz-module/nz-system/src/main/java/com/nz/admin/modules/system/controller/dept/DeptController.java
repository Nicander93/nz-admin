package com.nz.admin.modules.system.controller.dept;

import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.common.core.R;
import com.nz.admin.modules.system.entity.dataobject.dept.DeptDO;
import com.nz.admin.modules.system.service.dept.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/dept")
public class DeptController {

    @Autowired
    private DeptService deptService;

    @SaCheckPermission("system:dept:list")
    @GetMapping("/list")
    public R<List<DeptDO>> list() {
        return R.ok(deptService.listAll());
    }

    @SaCheckPermission("system:dept:query")
    @GetMapping("/{id}")
    public R<DeptDO> getById(@PathVariable Long id) {
        return R.ok(deptService.getById(id));
    }

    @SaCheckPermission("system:dept:add")
    @PostMapping
    public R<Void> add(@RequestBody DeptDO dept) {
        deptService.save(dept);
        return R.ok();
    }

    @SaCheckPermission("system:dept:edit")
    @PutMapping
    public R<Void> update(@RequestBody DeptDO dept) {
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
