package com.nz.admin.framework.web.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Web starter 配置。
 */
@Data
@ConfigurationProperties(prefix = "nz.web")
public class WebFrameworkProperties {

    private Cors cors = new Cors();
    private Swagger swagger = new Swagger();
    private Trace trace = new Trace();

    @Data
    public static class Cors {

        private boolean enabled = true;
        private List<String> allowedOriginPatterns = new ArrayList<>(List.of("*"));
        private List<String> allowedMethods = new ArrayList<>(List.of("*"));
        private List<String> allowedHeaders = new ArrayList<>(List.of("*"));
        private boolean allowCredentials = true;
        private long maxAge = 1800L;
    }

    @Data
    public static class Swagger {

        private boolean enabled = true;
        private String title = "NZ Admin API";
        private String description = "NZ Admin 接口文档";
        private String version = "v1.0.0";
        private String group = "default";
        private List<String> paths = new ArrayList<>(List.of("/api/**"));
    }

    @Data
    public static class Trace {

        private boolean enabled = true;
        private String requestHeaderName = "X-Request-Id";
        private String responseHeaderName = "X-Request-Id";
        private String mdcKey = "traceId";
    }
}
