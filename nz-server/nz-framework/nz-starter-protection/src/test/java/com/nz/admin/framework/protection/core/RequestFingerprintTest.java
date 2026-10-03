package com.nz.admin.framework.protection.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

class RequestFingerprintTest {
    @Test
    void ignoresMapKeyOrderButKeepsContentDifferences() {
        var first = new LinkedHashMap<String, Object>();
        first.put("a", 1);
        first.put("b", Map.of("c", 2));
        var second = new LinkedHashMap<String, Object>();
        second.put("b", Map.of("c", 2));
        second.put("a", 1);
        assertThat(RequestFingerprint.of(new Object[] {first}))
                .isEqualTo(RequestFingerprint.of(new Object[] {second}));
        second.put("a", 3);
        assertThat(RequestFingerprint.of(new Object[] {first}))
                .isNotEqualTo(RequestFingerprint.of(new Object[] {second}));
    }
}
