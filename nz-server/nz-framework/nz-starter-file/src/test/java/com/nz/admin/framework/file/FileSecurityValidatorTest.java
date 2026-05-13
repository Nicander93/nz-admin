package com.nz.admin.framework.file;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileSecurityValidatorTest {

    @Test
    void shouldNormalizeOriginalFilename() {
        FileSecurityValidator validator = new FileSecurityValidator(new FileStorageProperties());

        String normalized = validator.normalizeOriginalFilename("../abc 123?.png");

        assertThat(normalized).isEqualTo("abc_123_.png");
    }
}
