package com.nz.admin.framework.auth.annotation;

/**
 * 多权限校验模式。
 */
public enum PermissionMode {

    /** 任一权限命中即通过 */
    OR,
    /** 需要同时具备所有权限 */
    AND
}
