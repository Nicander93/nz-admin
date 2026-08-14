package com.nz.admin.modules.system.controller.auth;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.protection.annotation.RateLimit;
import com.nz.admin.framework.protection.annotation.RepeatSubmit;
import com.nz.admin.framework.social.core.SocialAuthorization;
import com.nz.admin.framework.social.core.SocialProvider;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dto.auth.SocialAuthorizeRequest;
import com.nz.admin.modules.system.entity.dto.auth.SocialCallbackRequest;
import com.nz.admin.modules.system.entity.vo.social.SocialCallbackVO;
import com.nz.admin.modules.system.service.auth.AuthenticationService;
import com.nz.admin.modules.system.service.social.SocialAccountService;
import com.nz.admin.modules.system.service.tenant.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 第三方登录的公开授权入口和统一回调。 */
@RestController
@RequestMapping("/api/auth/social")
public class SocialAuthController {

    private final SocialAccountService socialAccountService;
    private final TenantService tenantService;

    public SocialAuthController(SocialAccountService socialAccountService,
                                TenantService tenantService) {
        this.socialAccountService = socialAccountService;
        this.tenantService = tenantService;
    }

    @GetMapping("/providers")
    public R<List<SocialProvider>> providers() {
        return R.ok(socialAccountService.providers());
    }

    @RateLimit(permits = 10, windowSeconds = 60)
    @PostMapping("/authorize")
    public R<SocialAuthorization> authorize(@Valid @RequestBody SocialAuthorizeRequest request) {
        TenantDO tenant = tenantService.validateLoginTenant(request.tenantCode());
        return TenantContextHolder.callWithTenantId(tenant.getId(),
                () -> R.ok(socialAccountService.authorizeLogin(
                        tenant, request.clientId(), request.provider())));
    }

    @RepeatSubmit(intervalSeconds = 1)
    @RateLimit(permits = 10, windowSeconds = 60)
    @PostMapping("/callback")
    public R<SocialCallbackVO> callback(@Valid @RequestBody SocialCallbackRequest request,
                                        HttpServletRequest servletRequest) {
        return R.ok(socialAccountService.callback(
                request.provider(), request.code(), request.state(), metadata(servletRequest)));
    }

    private AuthenticationService.LoginMetadata metadata(HttpServletRequest request) {
        String ip = StrUtil.blankToDefault(request.getHeader("X-Forwarded-For"),
                request.getRemoteAddr());
        if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return new AuthenticationService.LoginMetadata(ip,
                StrUtil.blankToDefault(request.getHeader("User-Agent"), "unknown"));
    }
}
