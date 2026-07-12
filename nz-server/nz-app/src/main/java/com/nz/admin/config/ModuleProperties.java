package com.nz.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 部署期模块开关；修改后重启生效。
 */
@Data
@ConfigurationProperties(prefix = "nz.modules")
public class ModuleProperties {

    private Map<String, Switch> modules = new LinkedHashMap<>();

    @Data
    public static class Switch {
        private Boolean enabled;
    }

    public Boolean getEnabled(String code) {
        Switch value = modules.get(code);
        return value == null ? null : value.getEnabled();
    }
}
