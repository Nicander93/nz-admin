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
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 100)
@Component
public class SaCheckPermissionAspect {

    private final ObjectProvider<PermissionResolver> permissionResolvers;

    public SaCheckPermissionAspect(ObjectProvider<PermissionResolver> permissionResolverProvider) {
        this.permissionResolvers = permissionResolverProvider;
    }

    @Around("@annotation(com.nz.admin.framework.auth.annotation.SaCheckPermission) || @within(com.nz.admin.framework.auth.annotation.SaCheckPermission)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        SaCheckPermission annotation = resolveAnnotation(joinPoint);
        if (annotation != null) {
            checkPermission(annotation);
        }
        if (annotation == null) return joinPoint.proceed();
        var authorized = java.util.Arrays.stream(annotation.value()).filter(this::hasPermission)
                .collect(java.util.stream.Collectors.toSet());
        try (var scope = com.nz.admin.framework.auth.core.PermissionContext.open(authorized)) {
            return joinPoint.proceed();
        }
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
        var permissionResolver = permissionResolvers.getIfAvailable();
        if (permissionResolver != null) {
            permissionResolver.checkPermission(permission);
            return;
        }
        StpUtil.checkPermission(permission);
    }

    private boolean hasPermission(String permission) {
        var permissionResolver = permissionResolvers.getIfAvailable();
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
