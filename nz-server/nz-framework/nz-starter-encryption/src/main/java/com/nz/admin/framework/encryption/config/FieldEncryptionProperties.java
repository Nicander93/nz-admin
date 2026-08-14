package com.nz.admin.framework.encryption.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字段加密配置。
 */
@Data
@ConfigurationProperties(prefix = "nz.field-encryption")
public class FieldEncryptionProperties {

    private boolean enabled;

    private String activeKeyId = "v1";

    /**
     * 是否允许读取尚未迁移的明文。
     */
    private boolean allowPlaintextRead = true;

    /**
     * keyId 与密钥材料的映射，密钥材料至少 16 个字符。
     */
    private Map<String, String> keys = new LinkedHashMap<>();
}
