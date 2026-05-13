package com.nz.admin.framework.cache.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存 starter 配置。
 */
@Data
@ConfigurationProperties(prefix = "nz.cache")
public class CacheFrameworkProperties {

    private String keyPrefix = "nz-admin";
    private boolean cacheNullValues = false;
}
