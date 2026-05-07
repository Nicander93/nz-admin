package com.nz.admin.modules.system.controller;

import com.nz.admin.common.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.service.SysOnlineUserService;
import com.nz.admin.modules.system.entity.vo.OnlineUserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 在线用户控制器。
 */
@RestController
@RequestMapping("/api/system/online")
public class SysOnlineUserController {

    @Autowired
    private SysOnlineUserService onlineUserService;

    @SaCheckPermission("system:online:list")
    @GetMapping
    public R<List<OnlineUserVO>> list() {
        return R.ok(onlineUserService.listOnlineUsers());
    }

    @Log(title = "在线用户", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:online:force")
    @DeleteMapping("/{tokenValue}")
    public R<Void> forceLogout(@PathVariable String tokenValue) {
        onlineUserService.forceLogout(tokenValue);
        return R.ok();
    }
}
