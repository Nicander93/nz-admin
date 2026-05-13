package com.nz.admin.framework.test.core.ut;

import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.framework.test.core.util.TestContextCleaner;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 需要 Web 请求上下文和登录态的测试基类。
 */
public abstract class BaseWebContextUnitTest {

    protected void mockRequest(String method, String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    protected void mockLogin(Long userId) {
        if (RequestContextHolder.getRequestAttributes() == null) {
            mockRequest("GET", "/test");
        }
        StpUtil.login(userId);
    }

    @AfterEach
    void cleanupWebContext() {
        TestContextCleaner.clearAll();
    }
}
