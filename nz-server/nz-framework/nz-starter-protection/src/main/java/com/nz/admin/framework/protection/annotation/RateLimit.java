package com.nz.admin.framework.protection.annotation;

import java.lang.annotation.*;

/**
 * 接口限流。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    int permits() default 5;

    int windowSeconds() default 60;

    String key() default "";
}
