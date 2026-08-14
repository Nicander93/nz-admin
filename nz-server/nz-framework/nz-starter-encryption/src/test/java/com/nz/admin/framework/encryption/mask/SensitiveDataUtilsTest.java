package com.nz.admin.framework.encryption.mask;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataUtilsTest {

    @Test
    void shouldMaskPhone() {
        assertThat(SensitiveDataUtils.maskPhone("13800138000")).isEqualTo("138****8000");
        assertThat(SensitiveDataUtils.maskPhone("1234567")).isEqualTo("*******");
    }

    @Test
    void shouldMaskEmail() {
        assertThat(SensitiveDataUtils.maskEmail("admin@example.com")).isEqualTo("a****@example.com");
        assertThat(SensitiveDataUtils.maskEmail("ab")).isEqualTo("**");
    }
}
