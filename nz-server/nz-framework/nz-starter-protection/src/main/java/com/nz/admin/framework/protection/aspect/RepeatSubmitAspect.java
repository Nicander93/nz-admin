package com.nz.admin.framework.protection.aspect;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.core.CommonErrorCode;
import com.nz.admin.framework.protection.annotation.RepeatSubmit;
import com.nz.admin.framework.protection.core.InMemoryProtectionStore;
import com.nz.admin.framework.protection.core.ProtectionKeyResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 防重复提交切面。
 */
@Aspect
public class RepeatSubmitAspect {

    private final InMemoryProtectionStore protectionStore;
    private final ProtectionKeyResolver protectionKeyResolver;

    public RepeatSubmitAspect(InMemoryProtectionStore protectionStore, ProtectionKeyResolver protectionKeyResolver) {
        this.protectionStore = protectionStore;
        this.protectionKeyResolver = protectionKeyResolver;
    }

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatSubmit repeatSubmit) throws Throwable {
        HttpServletRequest request = currentRequest();
        String key = protectionKeyResolver.resolve("repeat-submit", repeatSubmit.key(), request);
        if (protectionStore.isRepeatSubmit(key, repeatSubmit.intervalSeconds())) {
            throw new BusinessException(CommonErrorCode.REPEAT_SUBMIT);
        }
        return joinPoint.proceed();
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
