package com.nz.admin.modules.system.service.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySmsVerificationCodeStoreTest {

    @Test
    void consumesCodeOnceAndLimitsAttempts() {
        InMemorySmsVerificationCodeStore store = new InMemorySmsVerificationCodeStore(
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

        assertThat(store.issue("1:13800138000", "good", Duration.ofMinutes(5),
                Duration.ofSeconds(60), 2))
                .isEqualTo(SmsVerificationCodeStore.IssueResult.ACCEPTED);
        assertThat(store.verifyAndConsume("1:13800138000", "bad"))
                .isEqualTo(SmsVerificationCodeStore.VerifyResult.INVALID);
        assertThat(store.verifyAndConsume("1:13800138000", "bad"))
                .isEqualTo(SmsVerificationCodeStore.VerifyResult.LOCKED);
        assertThat(store.verifyAndConsume("1:13800138000", "good"))
                .isEqualTo(SmsVerificationCodeStore.VerifyResult.MISSING);
    }

    @Test
    void rejectsImmediateResendAndConsumesSuccessfulCode() {
        InMemorySmsVerificationCodeStore store = new InMemorySmsVerificationCodeStore(
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

        store.issue("key", "good", Duration.ofMinutes(5), Duration.ofSeconds(60), 5);

        assertThat(store.issue("key", "new", Duration.ofMinutes(5), Duration.ofSeconds(60), 5))
                .isEqualTo(SmsVerificationCodeStore.IssueResult.TOO_FREQUENT);
        assertThat(store.verifyAndConsume("key", "good"))
                .isEqualTo(SmsVerificationCodeStore.VerifyResult.SUCCESS);
        assertThat(store.verifyAndConsume("key", "good"))
                .isEqualTo(SmsVerificationCodeStore.VerifyResult.MISSING);
    }
}
