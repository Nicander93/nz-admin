package com.nz.admin.framework.encryption.core;

import com.nz.admin.framework.encryption.config.FieldEncryptionProperties;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmFieldCipherTest {

    @Test
    void shouldUseRandomIvAndDecryptCiphertext() {
        AesGcmFieldCipher cipher = cipher("v2", true, Map.of(
                "v2", "test-field-key-material-v2"
        ));

        String first = cipher.encrypt("13800138000");
        String second = cipher.encrypt("13800138000");

        assertThat(first).startsWith("ENC$1$v2$").isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("13800138000");
        assertThat(cipher.activeKeyId()).isEqualTo("v2");
    }

    @Test
    void shouldReadOldKeyAndRewriteWithActiveKey() {
        AesGcmFieldCipher oldCipher = cipher("v1", true, Map.of(
                "v1", "test-field-key-material-v1"
        ));
        String oldValue = oldCipher.encrypt("admin@example.com");

        Map<String, String> rotatedKeys = new LinkedHashMap<>();
        rotatedKeys.put("v1", "test-field-key-material-v1");
        rotatedKeys.put("v2", "test-field-key-material-v2");
        AesGcmFieldCipher rotatedCipher = cipher("v2", true, rotatedKeys);

        assertThat(rotatedCipher.decrypt(oldValue)).isEqualTo("admin@example.com");
        assertThat(rotatedCipher.encrypt(oldValue)).startsWith("ENC$1$v2$");
    }

    @Test
    void shouldControlPlaintextCompatibility() {
        AesGcmFieldCipher compatible = cipher("v1", true, Map.of(
                "v1", "test-field-key-material-v1"
        ));
        AesGcmFieldCipher strict = cipher("v1", false, Map.of(
                "v1", "test-field-key-material-v1"
        ));

        assertThat(compatible.decrypt("legacy")).isEqualTo("legacy");
        assertThatThrownBy(() -> compatible.decrypt("ENC$1$broken"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("格式");
        assertThatThrownBy(() -> strict.decrypt("legacy"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未加密");
    }

    @Test
    void shouldRejectMissingActiveKey() {
        assertThatThrownBy(() -> cipher("v2", true, Map.of(
                "v1", "test-field-key-material-v1"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("活动字段加密密钥不存在");
    }

    private AesGcmFieldCipher cipher(String activeKeyId, boolean allowPlaintext, Map<String, String> keys) {
        FieldEncryptionProperties properties = new FieldEncryptionProperties();
        properties.setEnabled(true);
        properties.setActiveKeyId(activeKeyId);
        properties.setAllowPlaintextRead(allowPlaintext);
        properties.setKeys(new LinkedHashMap<>(keys));
        return new AesGcmFieldCipher(properties);
    }
}
