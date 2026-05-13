package com.nz.admin.modules.system.controller.user;

import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.convert.user.UserConvert;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.query.user.UserQuery;
import com.nz.admin.modules.system.entity.vo.user.UserVO;
import com.nz.admin.modules.system.service.permission.PermissionService;
import com.nz.admin.modules.system.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/system/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private PermissionService permissionService;

    @SaCheckPermission("system:user:list")
    @Log(title = "用户管理", businessType = BusinessType.QUERY)
    @GetMapping("/page")
    public R<PageResult<UserVO>> page(UserQuery query) {
        Page<UserDO> page = userService.listPage(query);
        return R.ok(PageResult.of(page, UserConvert.INSTANCE.toVOList(page.getRecords())));
    }

    @SaCheckPermission("system:user:query")
    @Log(title = "用户管理", businessType = BusinessType.QUERY)
    @GetMapping("/{id}")
    public R<UserVO> getById(@PathVariable Long id) {
        UserDO user = userService.getById(id);
        UserVO vo = UserConvert.INSTANCE.toVO(user);
        vo.setPostIds(userService.getPostIdsByUserId(id));
        return R.ok(vo);
    }

    @SaCheckPermission("system:user:add")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody UserDO user) {
        List<Long> postIds = user.getPostIds();
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        user.setPostIds(null);
        userService.save(user);
        userService.assignUserPosts(user.getId(), postIds != null ? postIds : Collections.emptyList());
        return R.ok();
    }

    @SaCheckPermission("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> update(@RequestBody UserDO user) {
        List<Long> postIds = user.getPostIds();
        if (StrUtil.isNotBlank(user.getPassword())) {
            user.setPassword(BCrypt.hashpw(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        user.setPostIds(null);
        userService.updateById(user);
        if (postIds != null) {
            userService.assignUserPosts(user.getId(), postIds);
        }
        return R.ok();
    }

    @SaCheckPermission("system:user:remove")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return R.ok();
    }

    @SaCheckPermission("system:user:edit")
    @GetMapping("/{userId}/roleIds")
    public R<List<Long>> getRoleIds(@PathVariable Long userId) {
        return R.ok(permissionService.getRoleIdsByUserId(userId));
    }

    @SaCheckPermission("system:user:edit")
    @PutMapping("/{userId}/roles")
    public R<Void> assignRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        permissionService.assignUserRoles(userId, roleIds);
        return R.ok();
    }

    @SaCheckPermission("system:user:resetPwd")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{userId}/password/reset")
    public R<Void> resetPassword(@PathVariable Long userId) {
        userService.resetPassword(userId);
        return R.ok();
    }
}
