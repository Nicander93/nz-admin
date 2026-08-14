package com.nz.admin.modules.system.service.auth;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.modules.system.entity.dataobject.client.ClientDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.service.client.ClientService;
import com.nz.admin.modules.system.service.dept.DeptService;
import com.nz.admin.modules.system.service.log.LoginLogService;
import com.nz.admin.modules.system.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuthenticationServiceImplTest {

    private UserService userService;
    private ClientService clientService;
    private SmsVerificationService smsVerificationService;
    private AuthenticationServiceImpl service;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        clientService = mock(ClientService.class);
        smsVerificationService = mock(SmsVerificationService.class);
        service = new AuthenticationServiceImpl(userService, clientService,
                mock(DeptService.class), mock(LoginLogService.class),
                Optional.of(smsVerificationService));
    }

    @Test
    void validatesSmsClientBeforeSendingCode() {
        when(clientService.getEnabledForLogin("web-sms", "sms"))
                .thenThrow(new BusinessException("客户端未启用"));

        assertThatThrownBy(() -> service.sendSmsLoginCode("web-sms", "13800138000"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("客户端未启用");

        verifyNoInteractions(smsVerificationService);
    }

    @Test
    void delegatesCodeSendingForEnabledSmsClient() {
        when(clientService.getEnabledForLogin("web-sms", "sms"))
                .thenReturn(new ClientDO().setClientId("web-sms").setLoginType("sms")
                        .setTokenTimeout(7200).setStatus(0));

        service.sendSmsLoginCode("web-sms", "13800138000");

        verify(smsVerificationService).sendLoginCode("13800138000");
    }

    @Test
    void rejectsDisabledUserAfterSmsVerification() {
        when(clientService.getEnabledForLogin("web-sms", "sms"))
                .thenReturn(new ClientDO().setClientId("web-sms").setLoginType("sms")
                        .setTokenTimeout(7200).setStatus(0));
        when(smsVerificationService.verifyLoginCode("13800138000", "123456"))
                .thenReturn(new UserDO().setId(7L).setUsername("disabled").setStatus(1));

        assertThatThrownBy(() -> service.loginBySms(null, "web-sms", "13800138000",
                "123456", new AuthenticationService.LoginMetadata("127.0.0.1", "test")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号已被禁用");
    }

    @Test
    void rejectsDisabledUserForSocialLogin() {
        UserDO user = new UserDO().setId(7L).setUsername("disabled").setStatus(1);
        when(clientService.getEnabledForLogin("nz-web-social", "social"))
                .thenReturn(new ClientDO().setClientId("nz-web-social")
                        .setLoginType("social").setTokenTimeout(7200).setStatus(0));

        assertThatThrownBy(() -> service.loginBySocial(
                null, "nz-web-social", user,
                new AuthenticationService.LoginMetadata("127.0.0.1", "test")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号已被禁用");

        verifyNoInteractions(userService);
    }
}
