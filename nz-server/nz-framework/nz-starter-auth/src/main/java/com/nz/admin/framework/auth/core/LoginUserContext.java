package com.nz.admin.framework.auth.core;

import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.common.core.identity.IdentitySnapshot;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 登录用户上下文工具。
 */
public class LoginUserContext {

    private final ObjectProvider<AuthUserResolver> authUserResolverProvider;

    public LoginUserContext(ObjectProvider<AuthUserResolver> authUserResolverProvider) {
        this.authUserResolverProvider = authUserResolverProvider;
    }

    public boolean isLogin() {
        try {
            return StpUtil.isLogin();
        } catch (Exception ignored) {
            return false;
        }
    }

    public Long getLoginUserIdOrNull() {
        if (!isLogin()) {
            return null;
        }
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return null;
        }
        return Long.parseLong(loginId.toString());
    }

    /** 当前令牌签发时的身份版本，用于长连接等无后续请求的场景。 */
    public IdentitySnapshot getIdentityOrNull() {
        Long userId = getLoginUserIdOrNull();
        return userId == null ? null : AuthSessionIdentity.read(userId, StpUtil.getTokenSession());
    }

    public LoginUser getLoginUserOrNull() {
        Long userId = getLoginUserIdOrNull();
        if (userId == null) {
            return null;
        }
        AuthUserResolver authUserResolver = authUserResolverProvider.getIfAvailable();
        if (authUserResolver == null) {
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(userId);
            return loginUser;
        }
        return authUserResolver.resolve(userId);
    }

    public String getUsernameOrDefault(String defaultValue) {
        LoginUser loginUser = getLoginUserOrNull();
        if (loginUser == null || loginUser.getUsername() == null || loginUser.getUsername().isBlank()) {
            Long userId = getLoginUserIdOrNull();
            return userId == null ? defaultValue : String.valueOf(userId);
        }
        return loginUser.getUsername();
    }
}
