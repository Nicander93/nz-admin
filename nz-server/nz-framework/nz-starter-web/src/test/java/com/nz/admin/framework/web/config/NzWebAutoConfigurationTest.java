package com.nz.admin.framework.web.config;

import com.nz.admin.framework.web.properties.WebFrameworkProperties;
import com.nz.admin.framework.web.support.RequestTraceFilter;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

class NzWebAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NzWebAutoConfiguration.class, SwaggerAutoConfiguration.class));

    @Test
    void shouldBindPropertiesAndRegisterDefaultBeans() {
        contextRunner.withPropertyValues(
                        "nz.web.swagger.title=NZ Admin Test API",
                        "nz.web.swagger.group=admin",
                        "nz.web.trace.response-header-name=X-Trace-Id"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(WebFrameworkProperties.class);
                    assertThat(context).hasSingleBean(WebMvcConfigurer.class);
                    assertThat(context).hasSingleBean(RequestTraceFilter.class);
                    assertThat(context).hasSingleBean(OpenAPI.class);
                    assertThat(context).hasSingleBean(GroupedOpenApi.class);

                    WebFrameworkProperties properties = context.getBean(WebFrameworkProperties.class);
                    assertThat(properties.getSwagger().getTitle()).isEqualTo("NZ Admin Test API");
                    assertThat(properties.getSwagger().getGroup()).isEqualTo("admin");
                    assertThat(properties.getTrace().getResponseHeaderName()).isEqualTo("X-Trace-Id");
                });
    }

    @Test
    void shouldDisableTraceAndSwaggerBeansWhenConfigured() {
        contextRunner.withPropertyValues(
                        "nz.web.trace.enabled=false",
                        "nz.web.swagger.enabled=false"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RequestTraceFilter.class);
                    assertThat(context).doesNotHaveBean(OpenAPI.class);
                    assertThat(context).doesNotHaveBean(GroupedOpenApi.class);
                });
    }
}
