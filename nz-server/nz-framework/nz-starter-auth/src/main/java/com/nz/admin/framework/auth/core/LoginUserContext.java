package com.nz.admin.framework.auth.core;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 登录用户上下文工具。
 */
public class LoginUserContext {

    private final AuthUserResolver authUserResolver;

    public LoginUserContext(ObjectProvider<AuthUserResolver> authUserResolverProvider) {
        this.authUserResolver = authUserResolverProvider.getIfAvailable();
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

    public LoginUser getLoginUserOrNull() {
        Long userId = getLoginUserIdOrNull();
        if (userId == null) {
            return null;
        }
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
