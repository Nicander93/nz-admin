package com.nz.admin.modules.system.service.auth;

import com.nz.admin.framework.cache.core.AtomicStateStore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

/** 发送频率与验证码生命周期分离，消费验证码不会释放发送频控。 */
public class SharedSmsVerificationCodeStore implements SmsVerificationCodeStore {
    private final AtomicStateStore states;

    public SharedSmsVerificationCodeStore(AtomicStateStore states) {
        this.states = states;
    }

    public IssueResult issue(
            String key, String hash, Duration ttl, Duration interval, int attempts) {
        if (attempts <= 0
                || ttl.isZero()
                || ttl.isNegative()
                || interval.isZero()
                || interval.isNegative()) throw new IllegalArgumentException("验证码参数不合法");
        if (!states.putIfAbsent("sms:send:" + key, "1", interval)) return IssueResult.TOO_FREQUENT;
        states.put(
                "sms:code:" + key,
                hash + ":" + attempts + ":" + (System.currentTimeMillis() + ttl.toMillis()),
                ttl);
        return IssueResult.ACCEPTED;
    }

    public VerifyResult verifyAndConsume(String key, String hash) {
        String name = "sms:code:" + key;
        for (int retry = 0; retry < 64; retry++) {
            String current = states.get(name);
            if (current == null) return VerifyResult.MISSING;
            String[] parts = current.split(":");
            int remaining = Integer.parseInt(parts[1]);
            long expiry = Long.parseLong(parts[2]);
            boolean match =
                    MessageDigest.isEqual(
                            parts[0].getBytes(StandardCharsets.UTF_8),
                            hash.getBytes(StandardCharsets.UTF_8));
            if (expiry <= System.currentTimeMillis() || match || remaining <= 1) {
                if (states.deleteIfValue(name, current))
                    return expiry <= System.currentTimeMillis()
                            ? VerifyResult.EXPIRED
                            : match ? VerifyResult.SUCCESS : VerifyResult.LOCKED;
            } else if (states.compareAndSet(
                    name,
                    current,
                    parts[0] + ":" + (remaining - 1) + ":" + expiry,
                    Duration.ofMillis(Math.max(1, expiry - System.currentTimeMillis()))))
                return VerifyResult.INVALID;
        }
        throw new IllegalStateException("验证码状态并发冲突，请重试");
    }

    public void invalidate(String key) {
        states.take("sms:code:" + key);
    }
}
