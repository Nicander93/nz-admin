package com.nz.admin.framework.social.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nz.admin.framework.cache.core.AtomicStateStore;

import java.time.Clock;
import java.time.Duration;

/** 跨节点授权状态，原子读取即删除。 */
public class SharedSocialAuthorizationStateStore implements SocialAuthorizationStateStore {
    private final AtomicStateStore states;
    private final ObjectMapper mapper;
    private final Clock clock;

    public SharedSocialAuthorizationStateStore(
            AtomicStateStore states, ObjectMapper mapper, Clock clock) {
        this.states = states;
        this.mapper = mapper;
        this.clock = clock;
    }

    public void save(String state, PendingSocialAuthorization authorization) {
        try {
            if (!states.putIfAbsent(
                    "social:" + state,
                    mapper.writeValueAsString(authorization),
                    Duration.between(clock.instant(), authorization.expiresAt())))
                throw new SocialAuthenticationException("授权状态已存在");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("授权状态无法序列化", e);
        }
    }

    public PendingSocialAuthorization consume(String state) {
        String value = states.take("social:" + state);
        if (value == null) throw new SocialAuthenticationException("第三方授权状态无效或已过期");
        try {
            var authorization = mapper.readValue(value, PendingSocialAuthorization.class);
            if (!authorization.expiresAt().isAfter(clock.instant()))
                throw new SocialAuthenticationException("第三方授权状态已过期");
            return authorization;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("授权状态无法读取", e);
        }
    }
}
