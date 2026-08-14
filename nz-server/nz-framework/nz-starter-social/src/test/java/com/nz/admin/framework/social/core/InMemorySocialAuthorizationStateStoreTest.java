package com.nz.admin.framework.social.core;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemorySocialAuthorizationStateStoreTest {

    private final Instant now = Instant.parse("2026-08-12T00:00:00Z");
    private final InMemorySocialAuthorizationStateStore store =
            new InMemorySocialAuthorizationStateStore(
                    Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void consumesStateOnlyOnce() {
        PendingSocialAuthorization pending = pending(now.plusSeconds(60));
        store.save("state", pending);

        assertThat(store.consume("state")).isSameAs(pending);
        assertThatThrownBy(() -> store.consume("state"))
                .isInstanceOf(SocialAuthenticationException.class)
                .hasMessage("第三方授权状态无效或已过期");
    }

    @Test
    void rejectsExpiredState() {
        store.save("expired", pending(now));

        assertThatThrownBy(() -> store.consume("expired"))
                .isInstanceOf(SocialAuthenticationException.class)
                .hasMessage("第三方授权状态无效或已过期");
    }

    private PendingSocialAuthorization pending(Instant expiresAt) {
        return new PendingSocialAuthorization(
                "github",
                new SocialAuthorizationContext(1L, "LOGIN", "nz-web-social", null),
                "verifier",
                "http://localhost/callback",
                expiresAt
        );
    }
}
