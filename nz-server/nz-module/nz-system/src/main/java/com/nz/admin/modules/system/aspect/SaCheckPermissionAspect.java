package com.nz.admin.modules.system.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.framework.auth.annotation.PermissionMode;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 自定义按钮权限校验切面。
 */
@Aspect
@Component
public class SaCheckPermissionAspect {

    /**
     * 拦截带权限注解的方法，按配置逻辑做校验。
     */
    @Around("@annotation(com.nz.admin.framework.auth.annotation.SaCheckPermission) || @within(com.nz.admin.framework.auth.annotation.SaCheckPermission)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        SaCheckPermission annotation = resolveAnnotation(joinPoint);
        if (annotation != null) {
            checkPermission(annotation);
        }
        return joinPoint.proceed();
    }

    private void checkPermission(SaCheckPermission annotation) {
        String[] permissions = annotation.value();
        if (permissions == null || permissions.length == 0) {
            return;
        }
        if (permissions.length == 1) {
            StpUtil.checkPermission(permissions[0]);
            return;
        }
        if (annotation.mode() == PermissionMode.AND) {
            for (String permission : permissions) {
                StpUtil.checkPermission(permission);
            }
            return;
        }
        for (String permission : permissions) {
            if (StpUtil.hasPermission(permission)) {
                return;
            }
        }
        StpUtil.checkPermission(permissions[0]);
    }

    private SaCheckPermission resolveAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SaCheckPermission annotation = AnnotationUtils.findAnnotation(method, SaCheckPermission.class);
        if (annotation != null) {
            return annotation;
        }
        return AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), SaCheckPermission.class);
    }
}
