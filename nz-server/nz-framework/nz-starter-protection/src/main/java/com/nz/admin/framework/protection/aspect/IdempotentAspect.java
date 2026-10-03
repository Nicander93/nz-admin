package com.nz.admin.framework.protection.aspect;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.protection.annotation.Idempotent;
import com.nz.admin.framework.protection.core.IdempotencyExecutor;
import com.nz.admin.framework.protection.core.ProtectionKeyResolver;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/** 在业务事务外层开启幂等事务，重试得到原来的成功结果。 */
@Aspect
@org.springframework.core.annotation.Order(
        org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 200)
public class IdempotentAspect {
    private final ObjectProvider<IdempotencyExecutor> executors;
    private final ProtectionKeyResolver keys;

    public IdempotentAspect(
            ObjectProvider<IdempotencyExecutor> executors, ProtectionKeyResolver keys) {
        this.executors = executors;
        this.keys = keys;
    }

    @Around("@annotation(annotation)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent annotation) throws Throwable {
        var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        var request = attributes == null ? null : attributes.getRequest();
        String key = request == null ? null : request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            if (annotation.required()) throw new BusinessException("缺少 Idempotency-Key");
            return joinPoint.proceed();
        }
        var executor = executors.getIfAvailable();
        if (executor == null) throw new IllegalStateException("业务幂等需要数据库事务支持");
        var method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String hash =
                com.nz.admin.framework.protection.core.RequestFingerprint.of(joinPoint.getArgs());
        return executor.execute(
                keys.resolve("idempotency", "", request),
                key,
                hash,
                method.getGenericReturnType(),
                Duration.ofSeconds(annotation.retentionSeconds()),
                () -> invoke(joinPoint));
    }

    @lombok.SneakyThrows
    private Object invoke(ProceedingJoinPoint joinPoint) {
        return joinPoint.proceed();
    }
}
