package com.nz.admin.framework.protection.annotation;

import java.lang.annotation.*;

/** 按 Idempotency-Key 重放成功响应；数据库变更与响应一起提交。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    int retentionSeconds() default 86400;

    boolean required() default false;
}
