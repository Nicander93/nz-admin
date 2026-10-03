package com.nz.admin.framework.auth.core;

import java.util.Set;

/** 当前已校验的功能权限，只在当前调用栈内有效。 */
public final class PermissionContext {
    private static final ThreadLocal<Set<String>> CURRENT = new ThreadLocal<>();

    private PermissionContext() {}

    public static Set<String> get() {
        var value = CURRENT.get();
        return value == null ? Set.of() : value;
    }

    public static Scope open(Set<String> permissions) {
        var previous = CURRENT.get();
        CURRENT.set(Set.copyOf(permissions));
        return () -> {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        };
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
