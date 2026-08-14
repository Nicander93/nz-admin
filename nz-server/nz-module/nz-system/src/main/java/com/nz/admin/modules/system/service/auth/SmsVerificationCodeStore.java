package com.nz.admin.modules.system.service.auth;

import java.time.Duration;

/** 短信验证码存储端口，可替换为 Redis 实现。 */
public interface SmsVerificationCodeStore {

    IssueResult issue(String key, String codeHash, Duration ttl, Duration resendInterval, int maxAttempts);

    VerifyResult verifyAndConsume(String key, String codeHash);

    void invalidate(String key);

    enum IssueResult {
        ACCEPTED,
        TOO_FREQUENT
    }

    enum VerifyResult {
        SUCCESS,
        INVALID,
        EXPIRED,
        LOCKED,
        MISSING
    }
}
