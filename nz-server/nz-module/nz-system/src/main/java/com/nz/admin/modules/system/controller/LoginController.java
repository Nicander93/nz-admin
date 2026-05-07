package com.nz.admin.modules.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.nz.admin.common.R;
import com.nz.admin.modules.system.entity.po.SysLoginLogDO;
import com.nz.admin.modules.system.entity.po.SysMenuDO;
import com.nz.admin.modules.system.entity.po.SysUserDO;
import com.nz.admin.modules.system.service.SysLoginLogService;
import com.nz.admin.modules.system.service.SysPermissionService;
import com.nz.admin.modules.system.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Autowired
    private SysUserService userService;
    @Autowired
    private SysPermissionService permissionService;
    @Autowired
    private SysLoginLogService loginLogService;

    public record LoginBody(String username, String password) {}

    @PostMapping("/login")
    public R<String> login(@RequestBody LoginBody body, HttpServletRequest request) {
        SysUserDO user = userService.getByUsername(body.username());
        if (user == null || !BCrypt.checkpw(body.password(), user.getPassword())) {
            return R.fail("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            return R.fail("账号已被禁用");
        }
        StpUtil.login(user.getId());
        SysLoginLogDO loginLog = new SysLoginLogDO();
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
        SysUserDO user = userService.getById(userId);
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
        List<SysMenuDO> userMenus = permissionService.getMenusByUserId(userId).stream()
                .filter(this::isMenuVisibleForRoute)
                .toList();

        Map<Long, UserMenu> menuMap = new LinkedHashMap<>();
        for (SysMenuDO menu : userMenus) {
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

    private boolean isMenuVisibleForRoute(SysMenuDO menu) {
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
