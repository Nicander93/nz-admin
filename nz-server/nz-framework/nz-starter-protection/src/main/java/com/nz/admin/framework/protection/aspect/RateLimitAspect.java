package com.nz.admin.framework.protection.aspect;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.core.CommonErrorCode;
import com.nz.admin.framework.protection.annotation.RateLimit;
import com.nz.admin.framework.protection.core.InMemoryProtectionStore;
import com.nz.admin.framework.protection.core.ProtectionKeyResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 本地限流切面。
 */
@Aspect
public class RateLimitAspect {

    private final InMemoryProtectionStore protectionStore;
    private final ProtectionKeyResolver protectionKeyResolver;

    public RateLimitAspect(InMemoryProtectionStore protectionStore, ProtectionKeyResolver protectionKeyResolver) {
        this.protectionStore = protectionStore;
        this.protectionKeyResolver = protectionKeyResolver;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = currentRequest();
        String key = protectionKeyResolver.resolve("rate-limit", rateLimit.key(), request);
        if (!protectionStore.tryAcquire(key, rateLimit.permits(), rateLimit.windowSeconds())) {
            throw new BusinessException(CommonErrorCode.TOO_MANY_REQUESTS);
        }
        return joinPoint.proceed();
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
