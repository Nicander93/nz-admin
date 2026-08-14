package com.nz.admin.modules.system.controller.online;

import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.service.online.OnlineUserService;
import com.nz.admin.modules.system.entity.vo.online.OnlineUserVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 在线用户控制器。
 */
@RestController
@RequestMapping("/api/system/online")
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    public OnlineUserController(OnlineUserService onlineUserService) {
        this.onlineUserService = onlineUserService;
    }

    @SaCheckPermission("system:online:list")
    @GetMapping
    public R<List<OnlineUserVO>> list(@RequestParam(required = false) String username,
                                      @RequestParam(required = false) String loginIp) {
        return R.ok(onlineUserService.listOnlineUsers(username, loginIp));
    }

    @Log(title = "在线用户", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:online:force")
    @DeleteMapping("/{tokenValue}")
    public R<Void> forceLogout(@PathVariable String tokenValue) {
        onlineUserService.forceLogout(tokenValue);
        return R.ok();
    }
}
