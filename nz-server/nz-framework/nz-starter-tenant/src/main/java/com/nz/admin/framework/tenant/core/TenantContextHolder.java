package com.nz.admin.framework.tenant.core;

import java.util.function.Supplier;

/**
 * 当前线程的租户上下文。
 */
public final class TenantContextHolder {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static Long getTenantIdOrNull() {
        return TENANT_ID.get();
    }

    public static void setTenantId(Long tenantId) {
        if (tenantId == null) {
            TENANT_ID.remove();
            return;
        }
        TENANT_ID.set(tenantId);
    }

    public static void clear() {
        TENANT_ID.remove();
    }

    public static void runWithTenantId(Long tenantId, Runnable action) {
        Long previous = TENANT_ID.get();
        setTenantId(tenantId);
        try {
            action.run();
        } finally {
            restore(previous);
        }
    }

    public static <T> T callWithTenantId(Long tenantId, Supplier<T> action) {
        Long previous = TENANT_ID.get();
        setTenantId(tenantId);
        try {
            return action.get();
        } finally {
            restore(previous);
        }
    }

    private static void restore(Long previous) {
        if (previous == null) {
            TENANT_ID.remove();
        } else {
            TENANT_ID.set(previous);
        }
    }
}
