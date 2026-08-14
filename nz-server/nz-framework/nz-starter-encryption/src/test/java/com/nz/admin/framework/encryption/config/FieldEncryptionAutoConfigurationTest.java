package com.nz.admin.framework.encryption.config;

import com.nz.admin.framework.encryption.core.AesGcmFieldCipher;
import com.nz.admin.framework.encryption.core.FieldCipher;
import com.nz.admin.framework.encryption.core.NoopFieldCipher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FieldEncryptionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FieldEncryptionAutoConfiguration.class));

    @Test
    void shouldUseNoopCipherByDefault() {
        contextRunner.run(context -> assertThat(context.getBean(FieldCipher.class))
                .isSameAs(NoopFieldCipher.INSTANCE));
    }

    @Test
    void shouldCreateAesCipherWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "nz.field-encryption.enabled=true",
                        "nz.field-encryption.active-key-id=v1",
                        "nz.field-encryption.keys.v1=test-field-key-material-v1"
                )
                .run(context -> assertThat(context.getBean(FieldCipher.class))
                        .isInstanceOf(AesGcmFieldCipher.class));
    }

    @Test
    void shouldFailWhenEnabledWithoutKey() {
        contextRunner
                .withPropertyValues("nz.field-encryption.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("字段加密");
                });
    }
}
