package com.nz.admin.framework.social.config;

import com.nz.admin.framework.social.core.SocialAuthorizationStateStore;
import com.nz.admin.framework.social.core.SocialOAuthService;
import com.nz.admin.framework.social.properties.SocialProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SocialAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SocialAutoConfiguration.class));

    @Test
    void registersProtocolAndStateStoreByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(SocialProperties.class);
            assertThat(context).hasSingleBean(SocialOAuthService.class);
            assertThat(context).hasSingleBean(SocialAuthorizationStateStore.class);
        });
    }

    @Test
    void canDisableSocialAuthentication() {
        runner.withPropertyValues("nz.social.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(SocialOAuthService.class));
    }
}
