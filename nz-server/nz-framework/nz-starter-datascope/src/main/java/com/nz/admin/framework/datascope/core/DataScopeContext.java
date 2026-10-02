package com.nz.admin.framework.datascope.core;

import java.util.function.Supplier;

/** 仅用于认证和范围解析的内部查询，不会关闭租户隔离。 */
public final class DataScopeContext {
    private static final ThreadLocal<Boolean> IGNORED = new ThreadLocal<>();

    private DataScopeContext() {}

    public static boolean isIgnored() {
        return Boolean.TRUE.equals(IGNORED.get());
    }

    public static <T> T withoutFilter(Supplier<T> action) {
        Boolean previous = IGNORED.get();
        IGNORED.set(true);
        try {
            return action.get();
        } finally {
            if (previous == null) IGNORED.remove();
            else IGNORED.set(previous);
        }
    }
}
