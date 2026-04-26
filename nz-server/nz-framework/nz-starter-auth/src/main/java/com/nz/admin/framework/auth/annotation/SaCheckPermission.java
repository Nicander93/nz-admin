package com.nz.admin.framework.auth.annotation;

import java.lang.annotation.*;

/**
 * 自定义按钮权限校验注解。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SaCheckPermission {

    /**
     * 权限标识，支持一个或多个。
     */
    String[] value();

    /**
     * 多权限组合逻辑。
     */
    PermissionMode mode() default PermissionMode.OR;
}
