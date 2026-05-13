package com.nz.admin.framework.test.core.util;

import cn.dev33.satoken.stp.StpUtil;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * 测试上下文清理工具。
 */
public final class TestContextCleaner {

    private TestContextCleaner() {
    }

    public static void clearAll() {
        clearRequestContext();
        clearMdc();
        clearSaToken();
    }

    public static void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    public static void clearMdc() {
        MDC.clear();
    }

    public static void clearSaToken() {
        try {
            StpUtil.logout();
        } catch (Exception ignored) {
        }
    }
}
