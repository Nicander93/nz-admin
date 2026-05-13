package com.nz.admin.framework.protection.annotation;

import java.lang.annotation.*;

/**
 * 防重复提交。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {

    int intervalSeconds() default 1;

    String key() default "";
}
