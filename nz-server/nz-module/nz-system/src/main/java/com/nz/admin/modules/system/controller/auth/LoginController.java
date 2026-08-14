package com.nz.admin.modules.system.controller.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.protection.annotation.RateLimit;
import com.nz.admin.framework.protection.annotation.RepeatSubmit;
import com.nz.admin.framework.realtime.core.RealtimeConnectionManager;
import com.nz.admin.framework.realtime.core.RealtimeTicketService;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.dto.auth.SmsCodeSendRequest;
import com.nz.admin.modules.system.entity.dto.auth.SmsLoginRequest;
import com.nz.admin.modules.system.service.auth.AuthenticationService;
import com.nz.admin.modules.system.service.permission.PermissionService;
import com.nz.admin.modules.system.service.tenant.TenantService;
import com.nz.admin.modules.system.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 统一登录、退出和当前用户信息接口。 */
@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private UserService userService;
    @Autowired
    private PermissionService permissionService;
    @Autowired(required = false)
    private RealtimeConnectionManager realtimeConnectionManager;
    @Autowired(required = false)
    private RealtimeTicketService realtimeTicketService;

    public record LoginBody(
            String tenantCode,
            String clientId,
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {
    }

    @RepeatSubmit(intervalSeconds = 1)
    @RateLimit(permits = 5, windowSeconds = 60)
    @PostMapping("/login")
    public R<String> login(@Valid @RequestBody LoginBody body, HttpServletRequest request) {
        String tenantCode = StrUtil.blankToDefault(body.tenantCode(), "default");
        TenantDO tenant = tenantService.validateLoginTenant(tenantCode);
        return TenantContextHolder.callWithTenantId(tenant.getId(),
                () -> R.ok(authenticationService.loginByPassword(
                        tenant,
                        StrUtil.blankToDefault(body.clientId(), "nz-web-account"),
                        body.username(),
                        body.password(),
                        metadata(request))));
    }

    @RateLimit(permits = 5, windowSeconds = 60)
    @PostMapping("/sms/code")
    public R<Void> sendSmsCode(@Valid @RequestBody SmsCodeSendRequest body) {
        TenantDO tenant = tenantService.validateLoginTenant(body.tenantCode());
        return TenantContextHolder.callWithTenantId(tenant.getId(), () -> {
            authenticationService.sendSmsLoginCode(body.clientId(), body.phone());
            return R.ok();
        });
    }

    @RepeatSubmit(intervalSeconds = 1)
    @RateLimit(permits = 5, windowSeconds = 60)
    @PostMapping("/sms/login")
    public R<String> smsLogin(@Valid @RequestBody SmsLoginRequest body, HttpServletRequest request) {
        TenantDO tenant = tenantService.validateLoginTenant(body.tenantCode());
        return TenantContextHolder.callWithTenantId(tenant.getId(),
                () -> R.ok(authenticationService.loginBySms(
                        tenant, body.clientId(), body.phone(), body.code(), metadata(request))));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            Long tenantId = TenantContextHolder.getTenantIdOrNull();
            if (tenantId != null) {
                revokeRealtimeAccess(tenantId, userId);
            }
        }
        StpUtil.logout();
        return R.ok();
    }

    private void revokeRealtimeAccess(Long tenantId, Long userId) {
        if (realtimeTicketService != null) {
            realtimeTicketService.revokeUser(tenantId, userId);
        }
        if (realtimeConnectionManager != null) {
            realtimeConnectionManager.disconnectUser(tenantId, userId);
        }
    }

    @GetMapping("/info")
    public R<Map<String, Object>> info() {
        long userId = StpUtil.getLoginIdAsLong();
        UserDO user = userService.getById(userId);
        user.setPassword(null);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("tenant", tenantService.getRequired(user.getTenantId()));
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

    private AuthenticationService.LoginMetadata metadata(HttpServletRequest request) {
        return new AuthenticationService.LoginMetadata(
                getClientIp(request),
                StrUtil.blankToDefault(request.getHeader("User-Agent"), "unknown"));
    }

    /** 获取客户端真实 IP 地址。 */
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
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /** 登录态返回给前端的菜单节点。 */
    public record UserMenu(
            Long id,
            String name,
            String path,
            String component,
            Long parentId,
            Map<String, Object> meta,
            List<UserMenu> children
    ) {
    }
}
