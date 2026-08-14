package com.nz.admin.framework.social.core;

import com.nz.admin.framework.social.properties.SocialProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultSocialOAuthServiceTest {

    @Test
    void createsPkceAuthorizationAndStoresTrustedContext() {
        SocialProperties properties = properties();
        Clock clock = Clock.fixed(Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC);
        InMemorySocialAuthorizationStateStore stateStore =
                new InMemorySocialAuthorizationStateStore(clock);
        DefaultSocialOAuthService service = new DefaultSocialOAuthService(
                properties, stateStore, RestClient.builder().build(), clock, new SecureRandom());
        SocialAuthorizationContext context =
                new SocialAuthorizationContext(9L, "LOGIN", "nz-web-social", null);

        SocialAuthorization authorization = service.authorize("github", context);

        assertThat(authorization.authorizeUrl())
                .contains("response_type=code", "client_id=client",
                        "code_challenge=", "code_challenge_method=S256",
                        "scope=read:user");
        PendingSocialAuthorization pending = stateStore.consume(authorization.state());
        assertThat(pending.context()).isEqualTo(context);
        assertThat(pending.codeVerifier()).hasSizeGreaterThan(43);
        assertThat(authorization.expiresAt()).isEqualTo(clock.instant().plusSeconds(300));
    }

    @Test
    void exposesOnlyEnabledProviders() {
        SocialProperties properties = properties();
        SocialProperties.Provider disabled = new SocialProperties.Provider();
        disabled.setDisplayName("Disabled");
        properties.getProviders().put("disabled", disabled);
        DefaultSocialOAuthService service = new DefaultSocialOAuthService(
                properties,
                new InMemorySocialAuthorizationStateStore(Clock.systemUTC()),
                RestClient.builder().build(),
                Clock.systemUTC(),
                new SecureRandom());

        assertThat(service.providers())
                .containsExactly(new SocialProvider("github", "GitHub"));
        assertThatThrownBy(() -> service.authorize("disabled",
                new SocialAuthorizationContext(1L, "LOGIN", "client", null)))
                .isInstanceOf(SocialAuthenticationException.class)
                .hasMessage("第三方登录服务商未启用");
    }

    private SocialProperties properties() {
        SocialProperties properties = new SocialProperties();
        SocialProperties.Provider provider = new SocialProperties.Provider();
        provider.setEnabled(true);
        provider.setDisplayName("GitHub");
        provider.setClientId("client");
        provider.setClientSecret("secret");
        provider.setAuthorizationUri("https://example.test/authorize");
        provider.setTokenUri("https://example.test/token");
        provider.setUserInfoUri("https://example.test/user");
        provider.setRedirectUri("http://localhost/callback");
        provider.setScopes(List.of("read:user"));
        properties.getProviders().put("github", provider);
        return properties;
    }
}
