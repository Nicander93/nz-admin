package com.nz.admin.framework.cache.core;

import com.nz.admin.framework.cache.properties.CacheFrameworkProperties;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 缓存 key 构造工具。
 */
public class CacheKeyBuilder {

    private final CacheFrameworkProperties properties;

    public CacheKeyBuilder(CacheFrameworkProperties properties) {
        this.properties = properties;
    }

    public String build(String... segments) {
        return Arrays.stream(segments)
                .filter(Objects::nonNull)
                .filter(segment -> !segment.isBlank())
                .collect(Collectors.joining(":", properties.getKeyPrefix() + ":", ""));
    }
}
