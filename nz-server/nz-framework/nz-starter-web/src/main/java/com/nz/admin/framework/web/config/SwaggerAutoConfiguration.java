package com.nz.admin.framework.web.config;

import com.nz.admin.framework.web.properties.WebFrameworkProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Swagger 自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(WebFrameworkProperties.class)
@ConditionalOnProperty(prefix = "nz.web.swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerAutoConfiguration {

    /**
     * 注册默认 OpenAPI 信息。
     *
     * @return OpenAPI 配置
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAPI openAPI(WebFrameworkProperties properties) {
        WebFrameworkProperties.Swagger swagger = properties.getSwagger();
        return new OpenAPI()
                .info(new Info()
                        .title(swagger.getTitle())
                        .description(swagger.getDescription())
                        .version(swagger.getVersion()));
    }

    @Bean
    @ConditionalOnMissingBean
    public GroupedOpenApi defaultGroupedOpenApi(WebFrameworkProperties properties) {
        WebFrameworkProperties.Swagger swagger = properties.getSwagger();
        return GroupedOpenApi.builder()
                .group(swagger.getGroup())
                .pathsToMatch(swagger.getPaths().toArray(String[]::new))
                .build();
    }
}
