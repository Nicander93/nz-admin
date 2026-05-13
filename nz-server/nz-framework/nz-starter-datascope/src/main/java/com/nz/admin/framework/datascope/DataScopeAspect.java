package com.nz.admin.framework.datascope;

import cn.dev33.satoken.exception.NotWebContextException;
import cn.dev33.satoken.stp.StpUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * 数据权限切面。
 */
@Aspect
public class DataScopeAspect {

    private final DataScopeUserResolver dataScopeUserResolver;

    public DataScopeAspect(DataScopeUserResolver dataScopeUserResolver) {
        this.dataScopeUserResolver = dataScopeUserResolver;
    }

    /**
     * 在服务方法执行前注入数据权限 SQL 条件。
     */
    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint joinPoint, DataScope dataScope) throws Throwable {
        if (!isLogin()) {
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

    private boolean isLogin() {
        try {
            return StpUtil.isLogin();
        } catch (NotWebContextException e) {
            // 非 Web 上下文（如单元测试），跳过数据权限检查
            return false;
        }
    }

    private String buildDataScopeSql(Long userId, DataScope dataScope) {
        DataScopeType[] scopes = dataScope.value();
        if (contains(scopes, DataScopeType.ALL) && dataScopeUserResolver.isAdmin(userId)) {
            return null;
        }

        Long deptId = dataScopeUserResolver.getDeptId(userId);
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
}
