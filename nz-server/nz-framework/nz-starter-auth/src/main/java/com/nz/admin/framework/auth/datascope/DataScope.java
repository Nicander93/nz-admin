package com.nz.admin.framework.auth.datascope;

import java.lang.annotation.*;

/**
 * 标记方法需要拼接数据权限条件。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 允许的数据范围。
     */
    DataScopeType[] value() default {DataScopeType.ALL, DataScopeType.DEPT, DataScopeType.SELF};

    /**
     * 部门字段名。
     */
    String deptAlias() default "dept_id";

    /**
     * 用户字段名。
     */
    String userAlias() default "id";
}
