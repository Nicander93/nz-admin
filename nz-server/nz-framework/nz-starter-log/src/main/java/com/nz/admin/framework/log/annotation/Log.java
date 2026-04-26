package com.nz.admin.framework.log.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 模块标题。
     */
    String title();

    /**
     * 操作类型。
     */
    BusinessType businessType() default BusinessType.QUERY;
}
