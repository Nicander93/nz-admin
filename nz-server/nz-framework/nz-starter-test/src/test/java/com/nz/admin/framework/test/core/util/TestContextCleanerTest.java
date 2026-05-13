package com.nz.admin.framework.test.core.util;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertNull;

class TestContextCleanerTest {

    @Test
    void shouldClearRequestContextAndMdc() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.put("traceId", "trace-1");

        TestContextCleaner.clearAll();

        assertNull(RequestContextHolder.getRequestAttributes());
        assertNull(MDC.get("traceId"));
    }
}
