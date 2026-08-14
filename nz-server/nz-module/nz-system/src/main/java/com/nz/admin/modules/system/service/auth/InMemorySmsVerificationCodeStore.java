package com.nz.admin.modules.system.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/** 单节点短信验证码存储。 */
public class InMemorySmsVerificationCodeStore implements SmsVerificationCodeStore {

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemorySmsVerificationCodeStore() {
        this(Clock.systemUTC());
    }

    InMemorySmsVerificationCodeStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public IssueResult issue(String key, String codeHash, Duration ttl,
                             Duration resendInterval, int maxAttempts) {
        Instant now = clock.instant();
        AtomicReference<IssueResult> result = new AtomicReference<>(IssueResult.ACCEPTED);
        entries.compute(key, (ignored, current) -> {
            if (current != null && current.nextSendAt().isAfter(now)) {
                result.set(IssueResult.TOO_FREQUENT);
                return current;
            }
            return new Entry(codeHash, now.plus(ttl), now.plus(resendInterval), maxAttempts);
        });
        return result.get();
    }

    @Override
    public VerifyResult verifyAndConsume(String key, String codeHash) {
        Instant now = clock.instant();
        AtomicReference<VerifyResult> result = new AtomicReference<>(VerifyResult.MISSING);
        entries.compute(key, (ignored, current) -> {
            if (current == null) {
                return null;
            }
            if (!current.expiresAt().isAfter(now)) {
                result.set(VerifyResult.EXPIRED);
                return null;
            }
            if (matches(current.codeHash(), codeHash)) {
                result.set(VerifyResult.SUCCESS);
                return null;
            }
            if (current.attemptsRemaining() <= 1) {
                result.set(VerifyResult.LOCKED);
                return null;
            }
            result.set(VerifyResult.INVALID);
            return new Entry(current.codeHash(), current.expiresAt(),
                    current.nextSendAt(), current.attemptsRemaining() - 1);
        });
        return result.get();
    }

    @Override
    public void invalidate(String key) {
        entries.remove(key);
    }

    private boolean matches(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private record Entry(
            String codeHash,
            Instant expiresAt,
            Instant nextSendAt,
            int attemptsRemaining
    ) {
    }
}
