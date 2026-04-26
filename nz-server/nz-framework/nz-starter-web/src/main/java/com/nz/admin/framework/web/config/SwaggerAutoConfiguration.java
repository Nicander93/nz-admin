package com.nz.admin.framework.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Swagger 自动配置。
 */
@AutoConfiguration
public class SwaggerAutoConfiguration {

    /**
     * 注册默认 OpenAPI 信息。
     *
     * @return OpenAPI 配置
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NZ Admin API")
                        .description("NZ Admin 接口文档")
                        .version("v1.0.0"));
    }
}
