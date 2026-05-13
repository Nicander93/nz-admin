package com.nz.admin.framework.auth.config;

import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.auth.properties.AuthFrameworkProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

class SaTokenAutoConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuthContextAutoConfiguration.class));
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuthContextAutoConfiguration.class, SaTokenAutoConfiguration.class));

    @Test
    void shouldBindPropertiesAndRegisterLoginUserContext() {
        contextRunner.withPropertyValues(
                        "nz.auth.include-paths[0]=/admin/**",
                        "nz.auth.exclude-paths[0]=/admin/login"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthFrameworkProperties.class);
                    assertThat(context).hasSingleBean(LoginUserContext.class);
                    assertThat(context).hasSingleBean(WebMvcConfigurer.class);

                    AuthFrameworkProperties properties = context.getBean(AuthFrameworkProperties.class);
                    assertThat(properties.getIncludePaths()).containsExactly("/admin/**");
                    assertThat(properties.getExcludePaths()).containsExactly("/admin/login");
                });
    }

    @Test
    void shouldRegisterLoginUserContextWithoutWebEnvironment() {
        applicationContextRunner.run(context -> {
            assertThat(context).hasSingleBean(LoginUserContext.class);
        });
    }
}
