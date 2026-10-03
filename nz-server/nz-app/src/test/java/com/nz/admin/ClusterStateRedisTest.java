package com.nz.admin;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.session.SaSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nz.admin.framework.auth.core.RedisSaTokenDao;
import com.nz.admin.framework.cache.core.RedisAtomicStateStore;
import com.nz.admin.framework.protection.core.SharedProtectionStore;
import com.nz.admin.framework.realtime.core.*;
import com.nz.admin.framework.social.core.*;
import com.nz.admin.modules.system.service.auth.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.time.*;
import java.util.UUID;

/** 两个独立连接与服务对象模拟两个应用节点，共享真实 Redis。 */
@EnabledIfEnvironmentVariable(named = "NZ_TEST_REDIS_PORT", matches = "[0-9]+")
class ClusterStateRedisTest {
    @Test
    void sessionsCodesStatesTicketsAndMessagesCrossNodes() throws Exception {
        int port = Integer.parseInt(System.getenv("NZ_TEST_REDIS_PORT"));
        var factoryA = new LettuceConnectionFactory("127.0.0.1", port);
        var factoryB = new LettuceConnectionFactory("127.0.0.1", port);
        factoryA.afterPropertiesSet();
        factoryB.afterPropertiesSet();
        var listener = new RedisMessageListenerContainer();
        String prefix = "cluster-test:" + UUID.randomUUID();
        try {
            var redisA = new StringRedisTemplate(factoryA);
            var redisB = new StringRedisTemplate(factoryB);
            var stateA = new RedisAtomicStateStore(redisA, prefix);
            var stateB = new RedisAtomicStateStore(redisB, prefix);
            var mapper = new ObjectMapper().findAndRegisterModules();
            var ttl = Duration.ofMinutes(1);
            var authA = new RedisSaTokenDao(redisA, prefix);
            var authB = new RedisSaTokenDao(redisB, prefix);
            authA.set("token", "7", 60);
            assertThat(authB.get("token")).isEqualTo("7");
            var session = new SaSession("session:7");
            session.set("tenant", 1L);
            session.set("loginTime", LocalDateTime.of(2026, 10, 3, 8, 0));
            authA.setSession(session, 60);
            assertThat(authB.getSession("session:7").get("tenant")).isEqualTo(1L);
            assertThat(authB.getSession("session:7").get("loginTime"))
                    .isEqualTo(LocalDateTime.of(2026, 10, 3, 8, 0));
            var loaded = authB.getSession("session:7");
            loaded.set("marker", "updated");
            authB.updateSession(loaded);
            assertThat(authA.getSession("session:7").get("marker")).isEqualTo("updated");
            assertThat(authA.getSessionTimeout("session:7")).isBetween(1L, 60L);
            authB.delete("token");
            assertThat(authA.get("token")).isNull();
            var smsA = new SharedSmsVerificationCodeStore(stateA);
            var smsB = new SharedSmsVerificationCodeStore(stateB);
            assertThat(smsA.issue("phone", "hash", ttl, ttl, 2))
                    .isEqualTo(SmsVerificationCodeStore.IssueResult.ACCEPTED);
            assertThat(smsB.verifyAndConsume("phone", "wrong"))
                    .isEqualTo(SmsVerificationCodeStore.VerifyResult.INVALID);
            assertThat(smsB.verifyAndConsume("phone", "hash"))
                    .isEqualTo(SmsVerificationCodeStore.VerifyResult.SUCCESS);
            assertThat(smsA.verifyAndConsume("phone", "hash"))
                    .isEqualTo(SmsVerificationCodeStore.VerifyResult.MISSING);
            assertThat(smsA.issue("phone", "hash", ttl, ttl, 2))
                    .isEqualTo(SmsVerificationCodeStore.IssueResult.TOO_FREQUENT);
            var socialA =
                    new SharedSocialAuthorizationStateStore(stateA, mapper, Clock.systemUTC());
            var socialB =
                    new SharedSocialAuthorizationStateStore(stateB, mapper, Clock.systemUTC());
            var pending =
                    new PendingSocialAuthorization(
                            "github",
                            new SocialAuthorizationContext(1L, "login", "web", null),
                            "verifier",
                            "http://localhost",
                            Instant.now().plus(ttl));
            socialA.save("once", pending);
            assertThat(socialB.consume("once")).isEqualTo(pending);
            assertThatThrownBy(() -> socialA.consume("once"))
                    .isInstanceOf(SocialAuthenticationException.class);
            var ticketA = new SharedRealtimeTicketService(stateA, mapper, ttl);
            var ticketB = new SharedRealtimeTicketService(stateB, mapper, ttl);
            var principal = new RealtimePrincipal(7L, 1L);
            String ticket = ticketA.issue(principal, RealtimeTransport.SSE);
            assertThat(ticketB.consume(ticket, RealtimeTransport.SSE)).contains(principal);
            assertThat(ticketA.consume(ticket, RealtimeTransport.SSE)).isEmpty();
            ticket = ticketA.issue(principal, RealtimeTransport.SSE);
            ticketB.revokeUser(1L, 7L);
            assertThat(ticketA.consume(ticket, RealtimeTransport.SSE)).isEmpty();
            var protectA = new SharedProtectionStore(stateA);
            var protectB = new SharedProtectionStore(stateB);
            assertThat(protectA.isRepeatSubmit("submit", 60)).isFalse();
            assertThat(protectB.isRepeatSubmit("submit", 60)).isTrue();
            var registryA = mock(RealtimeConnectionRegistry.class);
            var registryB = mock(RealtimeConnectionRegistry.class);
            var busA = new RedisRealtimePublisher(registryA, redisA, mapper, prefix + ":bus");
            var busB = new RedisRealtimePublisher(registryB, redisB, mapper, prefix + ":bus");
            listener.setConnectionFactory(factoryB);
            listener.addMessageListener(busB, new ChannelTopic(prefix + ":bus"));
            listener.afterPropertiesSet();
            listener.start();
            var message = RealtimeMessage.of("notice", "hello");
            busA.publishToUser(1L, 7L, message);
            verify(registryB, timeout(5000))
                    .publishToUser(eq(1L), eq(7L), argThat(m -> m.id().equals(message.id())));
            busA.disconnectUser(1L, 7L);
            verify(registryB, timeout(5000)).disconnectUser(1L, 7L);
        } finally {
            listener.destroy();
            factoryA.destroy();
            factoryB.destroy();
        }
    }
}
