package com.nz.admin.modules.system.controller.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.protection.annotation.RateLimit;
import com.nz.admin.framework.protection.annotation.RepeatSubmit;
import com.nz.admin.modules.system.entity.dataobject.log.LoginLogDO;
import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.service.log.LoginLogService;
import com.nz.admin.modules.system.service.permission.PermissionService;
import com.nz.admin.modules.system.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Autowired
    private UserService userService;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private LoginLogService loginLogService;

    public record LoginBody(String username, String password) {}

    @RepeatSubmit(intervalSeconds = 1, key = "auth:login")
    @RateLimit(permits = 5, windowSeconds = 60, key = "auth:login")
    @PostMapping("/login")
    public R<String> login(@RequestBody LoginBody body, HttpServletRequest request) {
        UserDO user = userService.getByUsername(body.username());
        if (user == null || !BCrypt.checkpw(body.password(), user.getPassword())) {
            saveLoginFail(body.username(), null, request, "用户名或密码错误");
            return R.fail("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            saveLoginFail(body.username(), user.getId(), request, "账号已被禁用");
            return R.fail("账号已被禁用");
        }
        StpUtil.login(user.getId());
        LoginLogDO loginLog = new LoginLogDO();
        loginLog.setUserId(user.getId());
        loginLog.setUsername(user.getUsername());
        loginLog.setIp(getClientIp(request));
        loginLog.setStatus(0);
        loginLog.setMsg("登录成功");
        loginLog.setLoginTime(LocalDateTime.now());
        loginLogService.saveAsync(loginLog);
        return R.ok(StpUtil.getTokenValue());
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @GetMapping("/info")
    public R<Map<String, Object>> info() {
        long userId = StpUtil.getLoginIdAsLong();
        UserDO user = userService.getById(userId);
        user.setPassword(null);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("roles", permissionService.getRoleKeysByUserId(userId));
        result.put("permissions", permissionService.getPermsByUserId(userId));
        return R.ok(result);
    }

    @GetMapping("/menus")
    public R<List<UserMenu>> menus() {
        long userId = StpUtil.getLoginIdAsLong();
        List<MenuDO> userMenus = permissionService.getMenusByUserId(userId).stream()
                .filter(this::isMenuVisibleForRoute)
                .toList();

        Map<Long, UserMenu> menuMap = new LinkedHashMap<>();
        for (MenuDO menu : userMenus) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("title", menu.getName());
            if (menu.getIcon() != null && !menu.getIcon().isBlank()) {
                meta.put("icon", menu.getIcon());
            }
            menuMap.put(menu.getId(), new UserMenu(
                    menu.getId(),
                    menu.getName(),
                    menu.getPath(),
                    menu.getComponent(),
                    menu.getParentId(),
                    meta,
                    new ArrayList<>()
            ));
        }

        List<UserMenu> roots = new ArrayList<>();
        for (UserMenu menu : menuMap.values()) {
            Long parentId = menu.parentId();
            if (parentId != null && parentId != 0L && menuMap.containsKey(parentId)) {
                menuMap.get(parentId).children().add(menu);
            } else {
                roots.add(menu);
            }
        }
        return R.ok(roots);
    }

    private boolean isMenuVisibleForRoute(MenuDO menu) {
        boolean enabled = menu.getStatus() == null || menu.getStatus() == 0;
        boolean visible = menu.getVisible() == null || menu.getVisible() == 0;
        boolean notButton = !"F".equalsIgnoreCase(menu.getType());
        return enabled && visible && notButton;
    }

    /**
     * 获取客户端真实 IP 地址。
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private void saveLoginFail(String username, Long userId, HttpServletRequest request, String msg) {
        LoginLogDO loginLog = new LoginLogDO();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setIp(getClientIp(request));
        loginLog.setStatus(1);
        loginLog.setMsg(msg);
        loginLog.setLoginTime(LocalDateTime.now());
        loginLogService.saveAsync(loginLog);
    }

    /**
     * 登录态返回给前端的菜单节点。
     */
    public record UserMenu(
            Long id,
            String name,
            String path,
            String component,
            Long parentId,
            Map<String, Object> meta,
            List<UserMenu> children
    ) {}
}
