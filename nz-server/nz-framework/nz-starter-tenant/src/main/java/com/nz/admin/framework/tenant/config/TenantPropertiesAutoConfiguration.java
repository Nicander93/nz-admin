package com.nz.admin.framework.tenant.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 无论是否启用行级隔离，都提供租户基础配置给业务层使用。
 */
@AutoConfiguration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantPropertiesAutoConfiguration {
}
