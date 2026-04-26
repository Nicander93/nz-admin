package com.nz.admin.framework.auth.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.framework.auth.datascope.DataScope;
import com.nz.admin.framework.auth.datascope.DataScopeParam;
import com.nz.admin.framework.auth.datascope.DataScopeType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

/**
 * 数据权限切面。
 */
@Aspect
@Component
public class DataScopeAspect {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 在服务方法执行前注入数据权限 SQL 条件。
     */
    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint joinPoint, DataScope dataScope) throws Throwable {
        if (!StpUtil.isLogin()) {
            return joinPoint.proceed();
        }
        Long userId = StpUtil.getLoginIdAsLong();
        String sql = buildDataScopeSql(userId, dataScope);
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof DataScopeParam dataScopeParam) {
                dataScopeParam.setDataScopeSql(sql);
            }
        }
        return joinPoint.proceed();
    }

    private String buildDataScopeSql(Long userId, DataScope dataScope) {
        DataScopeType[] scopes = dataScope.value();
        if (contains(scopes, DataScopeType.ALL) && isAdmin(userId)) {
            return null;
        }

        Long deptId = getCurrentUserDeptId(userId);
        if (deptId == null && !contains(scopes, DataScopeType.SELF)) {
            return "1 = 0";
        }
        if (contains(scopes, DataScopeType.DEPT) && deptId != null) {
            return dataScope.deptAlias() + " = " + deptId;
        }
        if (contains(scopes, DataScopeType.SELF)) {
            return dataScope.userAlias() + " = " + userId;
        }
        return "1 = 0";
    }

    private boolean isAdmin(Long userId) {
        Set<String> roleKeys = getRoleKeysByUserId(userId);
        if (roleKeys == null || roleKeys.isEmpty()) {
            return false;
        }
        return roleKeys.stream().filter(this::isNotBlank).anyMatch("admin"::equalsIgnoreCase);
    }

    @SuppressWarnings("unchecked")
    private Set<String> getRoleKeysByUserId(Long userId) {
        Object service = getBean("com.nz.admin.modules.system.service.SysPermissionService");
        if (service == null) {
            return Collections.emptySet();
        }
        Object result = invokeMethod(service, "getRoleKeysByUserId", new Class<?>[]{Long.class}, userId);
        if (!(result instanceof Set<?> set)) {
            return Collections.emptySet();
        }
        return (Set<String>) set;
    }

    private Long getCurrentUserDeptId(Long userId) {
        Object service = getBean("com.nz.admin.modules.system.service.SysUserService");
        if (service == null) {
            return null;
        }
        Object user = invokeMethod(service, "getById", new Class<?>[]{Long.class}, userId);
        if (user == null) {
            return null;
        }
        Object deptId = invokeMethod(user, "getDeptId", new Class<?>[0]);
        if (deptId instanceof Long value) {
            return value;
        }
        if (deptId instanceof Number value) {
            return value.longValue();
        }
        return null;
    }

    private Object getBean(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return applicationContext.getBean(clazz);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object invokeMethod(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean contains(DataScopeType[] scopes, DataScopeType target) {
        if (scopes == null || scopes.length == 0) {
            return false;
        }
        for (DataScopeType scope : scopes) {
            if (scope == target) {
                return true;
            }
        }
        return false;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
