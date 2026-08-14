package com.nz.admin.framework.file;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileConfigSecretCodecTest {

    @Test
    void encryptsWithRandomIvAndDecrypts() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setConfigEncryptionKey("test-key-at-least-for-unit-tests");
        FileConfigSecretCodec codec = new FileConfigSecretCodec(properties);

        String first = codec.encrypt("secret-value");
        String second = codec.encrypt("secret-value");

        assertThat(first).isNotEqualTo("secret-value").isNotEqualTo(second);
        assertThat(codec.decrypt(first)).isEqualTo("secret-value");
        assertThat(codec.decrypt(second)).isEqualTo("secret-value");
    }

    @Test
    void rejectsSecretWriteWithoutEncryptionKey() {
        FileConfigSecretCodec codec = new FileConfigSecretCodec(new FileStorageProperties());
        assertThatThrownBy(() -> codec.encrypt("secret-value"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("config-encryption-key");
    }
}
