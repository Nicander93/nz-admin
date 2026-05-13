package com.nz.admin.framework.log.aspect;

import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.framework.log.core.OperationLogEvent;
import com.nz.admin.framework.log.core.OperationLogRecorder;
import com.nz.admin.framework.log.properties.OperationLogProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLogAspectTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldRecordMaskedRequestAndResponse() {
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);
        LoginUserContext loginUserContext = mock(LoginUserContext.class);
        when(loginUserContext.getUsernameOrDefault("anonymous")).thenReturn("admin");

        OperationLogProperties properties = new OperationLogProperties();
        OperationLogAspect aspect = new OperationLogAspect(
                new StaticListableBeanFactory(Map.of("operationLogRecorder", recorder)).getBeanProvider(OperationLogRecorder.class),
                new StaticListableBeanFactory(Map.of("loginUserContext", loginUserContext)).getBeanProvider(LoginUserContext.class),
                properties
        );

        DemoService target = new DemoService();
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAspect(aspect);
        DemoService proxy = proxyFactory.getProxy();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/demo");
        request.addHeader("User-Agent", "JUnit");
        request.addHeader("X-Forwarded-For", "10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        proxy.create(new DemoRequest("secret", "hello"));

        ArgumentCaptor<OperationLogEvent> captor = ArgumentCaptor.forClass(OperationLogEvent.class);
        verify(recorder).record(captor.capture());
        OperationLogEvent event = captor.getValue();

        assertThat(event.getOperName()).isEqualTo("admin");
        assertThat(event.getOperIp()).isEqualTo("10.0.0.1");
        assertThat(event.getOperParam()).doesNotContain("secret");
        assertThat(event.getJsonResult()).contains("ok");
        assertThat(event.getStatus()).isEqualTo(0);
    }

    static class DemoService {

        @Log(title = "演示接口", businessType = BusinessType.INSERT)
        public Map<String, Object> create(DemoRequest request) {
            return Map.of("status", "ok", "password", "masked");
        }
    }

    record DemoRequest(String password, String name) {
    }
}
