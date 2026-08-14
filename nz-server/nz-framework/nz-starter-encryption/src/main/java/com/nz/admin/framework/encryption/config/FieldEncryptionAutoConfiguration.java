package com.nz.admin.framework.encryption.config;

import com.nz.admin.framework.encryption.core.AesGcmFieldCipher;
import com.nz.admin.framework.encryption.core.FieldCipher;
import com.nz.admin.framework.encryption.core.FieldCipherRegistrar;
import com.nz.admin.framework.encryption.core.NoopFieldCipher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 字段加密自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(FieldEncryptionProperties.class)
public class FieldEncryptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FieldCipher fieldCipher(FieldEncryptionProperties properties) {
        if (!properties.isEnabled()) {
            return NoopFieldCipher.INSTANCE;
        }
        return new AesGcmFieldCipher(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public FieldCipherRegistrar fieldCipherRegistrar(FieldCipher fieldCipher) {
        return new FieldCipherRegistrar(fieldCipher);
    }
}
