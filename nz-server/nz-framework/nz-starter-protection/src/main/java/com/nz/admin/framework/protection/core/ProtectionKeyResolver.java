package com.nz.admin.framework.protection.core;

import com.nz.admin.framework.auth.core.LoginUserContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 保护能力 key 解析器。
 */
public class ProtectionKeyResolver {

    private final LoginUserContext loginUserContext;

    public ProtectionKeyResolver(LoginUserContext loginUserContext) {
        this.loginUserContext = loginUserContext;
    }

    public String resolve(String prefix, String customKey, HttpServletRequest request) {
        String requestUri = request == null ? "unknown" : request.getRequestURI();
        if (customKey != null && !customKey.isBlank()) {
            return prefix + ":" + customKey;
        }
        Long userId = loginUserContext == null ? null : loginUserContext.getLoginUserIdOrNull();
        if (userId != null) {
            return prefix + ":" + requestUri + ":" + userId;
        }
        String remoteAddr = request == null ? "anonymous" : request.getRemoteAddr();
        return prefix + ":" + requestUri + ":" + remoteAddr;
    }
}
