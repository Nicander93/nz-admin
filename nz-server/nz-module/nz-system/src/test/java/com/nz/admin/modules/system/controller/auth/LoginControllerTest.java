package com.nz.admin.modules.system.controller.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.framework.realtime.core.RealtimeConnectionManager;
import com.nz.admin.framework.realtime.core.RealtimeTicketService;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dto.auth.SmsCodeSendRequest;
import com.nz.admin.modules.system.entity.dto.auth.SmsLoginRequest;
import com.nz.admin.modules.system.service.auth.AuthenticationService;
import com.nz.admin.modules.system.service.tenant.TenantService;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginControllerTest {

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void logoutRevokesRealtimeAccessBeforeInvalidatingToken() {
        RealtimeTicketService ticketService = mock(RealtimeTicketService.class);
        RealtimeConnectionManager connectionManager = mock(RealtimeConnectionManager.class);
        LoginController controller = new LoginController();
        ReflectionTestUtils.setField(controller, "realtimeTicketService", ticketService);
        ReflectionTestUtils.setField(controller, "realtimeConnectionManager", connectionManager);
        TenantContextHolder.setTenantId(3L);

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::isLogin).thenReturn(true);
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(7L);

            var result = controller.logout();

            assertThat(result.getCode()).isEqualTo(200);
            verify(ticketService).revokeUser(3L, 7L);
            verify(connectionManager).disconnectUser(3L, 7L);
            stpUtil.verify(StpUtil::logout);
        }
    }

    @Test
    void sendsSmsCodeInsideValidatedTenant() {
        LoginController controller = new LoginController();
        TenantService tenantService = mock(TenantService.class);
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        ReflectionTestUtils.setField(controller, "tenantService", tenantService);
        ReflectionTestUtils.setField(controller, "authenticationService", authenticationService);
        when(tenantService.validateLoginTenant("tenant-a"))
                .thenReturn(new TenantDO().setId(9L).setTenantCode("tenant-a"));
        doAnswer(invocation -> {
            assertThat(TenantContextHolder.getTenantIdOrNull()).isEqualTo(9L);
            return null;
        }).when(authenticationService).sendSmsLoginCode("nz-web-sms", "13800138000");

        var result = controller.sendSmsCode(
                new SmsCodeSendRequest("tenant-a", "nz-web-sms", "13800138000"));

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(TenantContextHolder.getTenantIdOrNull()).isNull();
    }

    @Test
    void logsInBySmsWithServerRequestMetadata() {
        LoginController controller = new LoginController();
        TenantService tenantService = mock(TenantService.class);
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        ReflectionTestUtils.setField(controller, "tenantService", tenantService);
        ReflectionTestUtils.setField(controller, "authenticationService", authenticationService);
        TenantDO tenant = new TenantDO().setId(9L).setTenantCode("tenant-a");
        when(tenantService.validateLoginTenant("tenant-a")).thenReturn(tenant);
        when(authenticationService.loginBySms(eq(tenant), eq("nz-web-sms"),
                eq("13800138000"), eq("123456"), any(AuthenticationService.LoginMetadata.class)))
                .thenReturn("sms-token");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.2");
        request.addHeader("User-Agent", "sms-test");

        var result = controller.smsLogin(
                new SmsLoginRequest("tenant-a", "nz-web-sms", "13800138000", "123456"), request);

        assertThat(result.getData()).isEqualTo("sms-token");
        assertThat(TenantContextHolder.getTenantIdOrNull()).isNull();
    }
}
