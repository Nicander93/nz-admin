package com.nz.admin.framework.protection.aspect;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.protection.annotation.RateLimit;
import com.nz.admin.framework.protection.core.ProtectionKeyResolver;
import com.nz.admin.framework.protection.core.ProtectionStore;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RateLimitAspectTest {
    @AfterEach
    void clearRequest() { RequestContextHolder.resetRequestAttributes(); }

    @Test
    void rejectedRequestKeepsTheLimitAndAdvertisesAConservativeRetryWindow() throws Throwable {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        var response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        var store = mock(ProtectionStore.class);
        var invocation = mock(ProceedingJoinPoint.class);
        var annotation = limit();
        var aspect = new RateLimitAspect(store, new ProtectionKeyResolver(anonymousUser()));

        assertThatThrownBy(() -> aspect.around(invocation, annotation)).isInstanceOf(BusinessException.class);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        verify(store).tryAcquire("rate-limit:POST:/api/auth/login:127.0.0.1", 5, 60);
        verify(invocation, never()).proceed();
    }

    @Test
    void acceptedRequestDoesNotReceiveRetryAdvice() throws Throwable {
        var response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest(), response));
        var store = mock(ProtectionStore.class);
        when(store.tryAcquire(anyString(), eq(5), eq(60))).thenReturn(true);
        var invocation = mock(ProceedingJoinPoint.class);
        when(invocation.proceed()).thenReturn("accepted");
        var aspect = new RateLimitAspect(store, new ProtectionKeyResolver(anonymousUser()));

        assertThat(aspect.around(invocation, limit())).isEqualTo("accepted");
        assertThat(response.getHeader("Retry-After")).isNull();
    }

    private LoginUserContext anonymousUser() {
        var context = mock(LoginUserContext.class);
        when(context.getLoginUserIdOrNull()).thenReturn(null);
        return context;
    }

    private RateLimit limit() {
        var result = mock(RateLimit.class);
        when(result.key()).thenReturn("");
        when(result.permits()).thenReturn(5);
        when(result.windowSeconds()).thenReturn(60);
        return result;
    }
}
