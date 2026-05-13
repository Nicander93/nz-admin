package com.nz.admin.modules.system.controller.dept;

import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.common.core.R;
import com.nz.admin.modules.system.entity.dataobject.dept.PostDO;
import com.nz.admin.modules.system.service.dept.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/post")
public class PostController {

    @Autowired
    private PostService postService;

    @SaCheckPermission("system:post:list")
    @GetMapping("/list")
    public R<List<PostDO>> list() {
        return R.ok(postService.listAll());
    }

    @SaCheckPermission("system:post:query")
    @GetMapping("/{id}")
    public R<PostDO> getById(@PathVariable Long id) {
        return R.ok(postService.getById(id));
    }

    @Log(title = "岗位管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:post:add")
    @PostMapping
    public R<Void> add(@RequestBody PostDO post) {
        postService.save(post);
        return R.ok();
    }

    @Log(title = "岗位管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:post:edit")
    @PutMapping
    public R<Void> update(@RequestBody PostDO post) {
        postService.updateById(post);
        return R.ok();
    }

    @Log(title = "岗位管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:post:remove")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        postService.removeById(id);
        return R.ok();
    }
}
