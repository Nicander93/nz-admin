package com.nz.admin.framework.social.core;

/** OAuth 授权状态存储，可替换为 Redis 实现以支持集群部署。 */
public interface SocialAuthorizationStateStore {
    void save(String state, PendingSocialAuthorization authorization);

    PendingSocialAuthorization consume(String state);
}
