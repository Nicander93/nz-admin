package com.nz.admin.common.job;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 显式开放给调度器的 public 无参任务方法。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JobTask {
    /** 仅对已确保数据库事务或下游幂等的任务开放崩溃后重试。 */
    boolean recoverable() default false;
}
