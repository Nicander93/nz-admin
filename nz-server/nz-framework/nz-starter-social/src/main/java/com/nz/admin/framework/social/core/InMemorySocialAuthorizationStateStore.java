package com.nz.admin.framework.social.core;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 单节点默认 state 存储；读取即删除，避免授权码回放。 */
public class InMemorySocialAuthorizationStateStore implements SocialAuthorizationStateStore {
    private final Clock clock;
    private final Map<String, PendingSocialAuthorization> states = new ConcurrentHashMap<>();

    public InMemorySocialAuthorizationStateStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void save(String state, PendingSocialAuthorization authorization) {
        states.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(clock.instant()));
        states.put(state, authorization);
    }

    @Override
    public PendingSocialAuthorization consume(String state) {
        PendingSocialAuthorization authorization = states.remove(state);
        if (authorization == null || !authorization.expiresAt().isAfter(clock.instant())) {
            throw new SocialAuthenticationException("第三方授权状态无效或已过期");
        }
        return authorization;
    }
}
