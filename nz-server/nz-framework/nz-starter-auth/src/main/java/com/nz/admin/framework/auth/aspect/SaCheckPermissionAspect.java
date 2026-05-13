package com.nz.admin.framework.auth.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.framework.auth.annotation.PermissionMode;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.auth.core.PermissionResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 自定义按钮权限校验切面。
 */
@Aspect
@Component
public class SaCheckPermissionAspect {

    private final PermissionResolver permissionResolver;

    public SaCheckPermissionAspect(ObjectProvider<PermissionResolver> permissionResolverProvider) {
        this.permissionResolver = permissionResolverProvider.getIfAvailable();
    }

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
            checkPermission(permissions[0]);
            return;
        }
        if (annotation.mode() == PermissionMode.AND) {
            for (String permission : permissions) {
                checkPermission(permission);
            }
            return;
        }
        for (String permission : permissions) {
            if (hasPermission(permission)) {
                return;
            }
        }
        checkPermission(permissions[0]);
    }

    private void checkPermission(String permission) {
        if (permissionResolver != null) {
            permissionResolver.checkPermission(permission);
            return;
        }
        StpUtil.checkPermission(permission);
    }

    private boolean hasPermission(String permission) {
        if (permissionResolver != null) {
            return permissionResolver.hasPermission(permission);
        }
        return StpUtil.hasPermission(permission);
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
