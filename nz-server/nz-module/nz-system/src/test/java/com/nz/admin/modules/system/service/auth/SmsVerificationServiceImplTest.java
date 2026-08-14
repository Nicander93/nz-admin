package com.nz.admin.modules.system.service.auth;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.config.SmsVerificationProperties;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.service.sms.SmsService;
import com.nz.admin.modules.system.service.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class SmsVerificationServiceImplTest {

    private SmsService smsService;
    private UserService userService;
    private SmsVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        SmsVerificationProperties properties = new SmsVerificationProperties();
        properties.setResendInterval(Duration.ofSeconds(60));
        smsService = mock(SmsService.class);
        userService = mock(UserService.class);
        service = new SmsVerificationServiceImpl(new InMemorySmsVerificationCodeStore(),
                properties, Optional.of(smsService), userService);
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void sendsAndConsumesGeneratedCode() {
        UserDO user = new UserDO().setId(7L).setUsername("mobile").setStatus(0);
        when(userService.getByPhone("13800138000")).thenReturn(user);
        ArgumentCaptor<Map<String, Object>> parameters = ArgumentCaptor.forClass(Map.class);

        service.sendLoginCode("13800138000");

        verify(smsService).sendByTemplateCode(eq("13800138000"),
                eq("verification-code"), parameters.capture());
        String code = String.valueOf(parameters.getValue().get("code"));
        assertThat(code).matches("\\d{6}");
        assertThat(service.verifyLoginCode("13800138000", code)).isSameAs(user);
        assertThatThrownBy(() -> service.verifyLoginCode("13800138000", code))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码错误");
    }

    @Test
    void doesNotRevealOrSendForUnknownPhone() {
        when(userService.getByPhone("13900139000")).thenReturn(null);

        service.sendLoginCode("13900139000");

        verifyNoInteractions(smsService);
    }

    @Test
    void invalidatesCodeWhenProviderFails() {
        when(userService.getByPhone("13800138000"))
                .thenReturn(new UserDO().setId(7L).setStatus(0));
        when(smsService.sendByTemplateCode(anyString(), anyString(), anyMap()))
                .thenThrow(new BusinessException("供应商失败"));

        assertThatThrownBy(() -> service.sendLoginCode("13800138000"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("供应商失败");
        assertThatThrownBy(() -> service.verifyLoginCode("13800138000", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码错误");
    }
}
