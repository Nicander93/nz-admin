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

    /**
     * 是否记录请求参数。
     */
    boolean recordRequest() default true;

    /**
     * 是否记录响应结果。
     */
    boolean recordResponse() default true;
}
