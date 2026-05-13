package com.nz.admin.framework.auth.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证 starter 配置。
 */
@Data
@ConfigurationProperties(prefix = "nz.auth")
public class AuthFrameworkProperties {

    private List<String> includePaths = new ArrayList<>(List.of("/api/**"));
    private List<String> excludePaths = new ArrayList<>(List.of("/api/auth/login"));
}
