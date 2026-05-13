package com.nz.admin.framework.auth.core;

/**
 * 权限解析扩展点。
 */
public interface PermissionResolver {

    void checkPermission(String permission);

    boolean hasPermission(String permission);

    default void checkRole(String role) {
    }

    default boolean hasRole(String role) {
        return false;
    }
}
