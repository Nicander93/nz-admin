package com.nz.admin.framework.protection.aspect;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.protection.annotation.RateLimit;
import com.nz.admin.framework.protection.annotation.RepeatSubmit;
import com.nz.admin.framework.protection.core.InMemoryProtectionStore;
import com.nz.admin.framework.protection.core.ProtectionKeyResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtectionAspectTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldBlockRepeatSubmit() {
        DemoService proxy = createProxy();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest("POST", "/api/demo/repeat")));

        proxy.repeat();
        BusinessException exception = assertThrows(BusinessException.class, proxy::repeat);
        assertEquals(429, exception.getCode());
    }

    @Test
    void shouldBlockWhenRateLimitExceeded() {
        DemoService proxy = createProxy();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest("POST", "/api/demo/rate")));

        proxy.rate();
        BusinessException exception = assertThrows(BusinessException.class, proxy::rate);
        assertEquals(429, exception.getCode());
    }

    private DemoService createProxy() {
        InMemoryProtectionStore store = new InMemoryProtectionStore();
        ProtectionKeyResolver keyResolver = new ProtectionKeyResolver(null);
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new DemoService());
        proxyFactory.addAspect(new RepeatSubmitAspect(store, keyResolver));
        proxyFactory.addAspect(new RateLimitAspect(store, keyResolver));
        return proxyFactory.getProxy();
    }

    static class DemoService {

        @RepeatSubmit(intervalSeconds = 5)
        public void repeat() {
        }

        @RateLimit(permits = 1, windowSeconds = 60)
        public void rate() {
        }
    }
}
