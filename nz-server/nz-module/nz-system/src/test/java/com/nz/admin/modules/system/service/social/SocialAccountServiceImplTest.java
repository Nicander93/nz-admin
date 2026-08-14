package com.nz.admin.modules.system.service.social;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.social.core.SocialAuthorization;
import com.nz.admin.framework.social.core.SocialAuthorizationContext;
import com.nz.admin.framework.social.core.SocialCallbackResult;
import com.nz.admin.framework.social.core.SocialIdentity;
import com.nz.admin.framework.social.core.SocialOAuthService;
import com.nz.admin.modules.system.entity.dataobject.client.ClientDO;
import com.nz.admin.modules.system.entity.dataobject.social.SocialBindingDO;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.mapper.social.SocialBindingMapper;
import com.nz.admin.modules.system.service.auth.AuthenticationService;
import com.nz.admin.modules.system.service.client.ClientService;
import com.nz.admin.modules.system.service.tenant.TenantService;
import com.nz.admin.modules.system.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SocialAccountServiceImplTest {

    private SocialOAuthService socialOAuthService;
    private ClientService clientService;
    private AuthenticationService authenticationService;
    private SocialBindingMapper bindingMapper;
    private SocialAccountServiceImpl service;

    @BeforeEach
    void setUp() {
        socialOAuthService = mock(SocialOAuthService.class);
        clientService = mock(ClientService.class);
        authenticationService = mock(AuthenticationService.class);
        bindingMapper = mock(SocialBindingMapper.class);
        service = new SocialAccountServiceImpl(
                socialOAuthService,
                clientService,
                mock(TenantService.class),
                mock(UserService.class),
                authenticationService
        );
        ReflectionTestUtils.setField(service, "baseMapper", bindingMapper);
    }

    @Test
    void validatesSocialClientAndStoresLoginContext() {
        TenantDO tenant = new TenantDO().setId(9L);
        when(clientService.getEnabledForLogin("nz-web-social", "social"))
                .thenReturn(new ClientDO().setClientId("nz-web-social"));
        SocialAuthorization authorization = new SocialAuthorization(
                "https://example.test/authorize", "state", Instant.now().plusSeconds(60));
        when(socialOAuthService.authorize(eq("github"), any())).thenReturn(authorization);

        assertThat(service.authorizeLogin(tenant, "nz-web-social", "github"))
                .isSameAs(authorization);

        ArgumentCaptor<SocialAuthorizationContext> context =
                ArgumentCaptor.forClass(SocialAuthorizationContext.class);
        verify(socialOAuthService).authorize(eq("github"), context.capture());
        assertThat(context.getValue()).isEqualTo(
                new SocialAuthorizationContext(9L, "LOGIN", "nz-web-social", null));
    }

    @Test
    void rejectsLoginWhenThirdPartyIdentityIsNotBound() {
        SocialIdentity identity = new SocialIdentity(
                "github", "42", "octocat", "Octocat", null, null);
        when(socialOAuthService.callback("github", "code", "state"))
                .thenReturn(new SocialCallbackResult(
                        new SocialAuthorizationContext(1L, "LOGIN", "nz-web-social", null),
                        identity));
        when(bindingMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.callback(
                "github", "code", "state",
                new AuthenticationService.LoginMetadata("127.0.0.1", "test")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该第三方账号尚未绑定系统用户");
        verifyNoInteractions(authenticationService);
    }

    @Test
    void rejectsUnbindingAnotherUsersAccount() {
        when(bindingMapper.selectById(8L)).thenReturn(
                new SocialBindingDO().setId(8L).setUserId(2L));

        assertThatThrownBy(() -> service.unbind(1L, 8L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("第三方账号绑定不存在");
        verify(bindingMapper).selectById(8L);
        verifyNoMoreInteractions(bindingMapper);
    }
}
