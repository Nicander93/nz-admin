package com.nz.admin;

import cn.hutool.crypto.digest.BCrypt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nz.admin.framework.datascope.core.DataScopeContext;
import com.nz.admin.framework.realtime.core.RealtimeMessage;
import com.nz.admin.framework.realtime.core.RedisRealtimePublisher;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dto.message.MessageSendRequest;
import com.nz.admin.modules.system.service.message.MessageService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** 真实 PostgreSQL 消息收件箱、授权边界和提交后实时投递回归。 */
@EnabledIfEnvironmentVariable(named = "NZ_TEST_PG_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "NZ_TEST_REDIS_PORT", matches = "[0-9]+")
@SpringBootTest(
        classes = NzAdminApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "nz.cluster.enabled=true",
                "nz.redis.enabled=true",
                "spring.data.redis.port=${NZ_TEST_REDIS_PORT}"
        })
@ActiveProfiles("pg-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MessageLifecyclePostgresTest {

    private static final String CACHE_KEY_PREFIX = "message-lifecycle-" + UUID.randomUUID().toString().replace("-", "");
    private static final String PASSWORD = "message-lifecycle-password";
    private static final long TENANT_ID = 980400L;
    private static final long FOREIGN_TENANT_ID = 980401L;
    private static final long SENDER_ID = 980410L;
    private static final long RECIPIENT_ONE_ID = 980411L;
    private static final long RECIPIENT_TWO_ID = 980412L;
    private static final long FOREIGN_SENDER_ID = 980413L;
    private static final long FOREIGN_RECIPIENT_ID = 980414L;
    private static final long ROLE_ID = 980420L;
    private static final long FOREIGN_ROLE_ID = 980421L;
    private static final long PACKAGE_ID = 980430L;
    private static final List<String> MESSAGE_PERMISSIONS = List.of(
            "system:message:list", "system:message:query", "system:message:read",
            "system:message:remove", "system:message:send");

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;
    @Autowired StringRedisTemplate redis;
    @Autowired ObjectMapper mapper;
    @Autowired MessageService messageService;

    private final String suffix = UUID.randomUUID().toString().replace("-", "");
    private final String tenantCode = "message-" + suffix.substring(0, 12);
    private final String foreignTenantCode = "message-other-" + suffix.substring(0, 8);
    private final String source = "message-lifecycle-" + suffix;
    private final String channel = CACHE_KEY_PREFIX + ":realtime";
    private long previousLoginAt;
    private Session sender;
    private Session recipientOne;
    private Session recipientTwo;

    @DynamicPropertySource
    static void isolateCacheNamespace(DynamicPropertyRegistry registry) {
        registry.add("nz.cache.key-prefix", () -> CACHE_KEY_PREFIX);
    }

    @BeforeAll
    void seedFixturesAndLoginOnce() {
        cleanupFixtures();
        seedFixtures();
        sender = login(tenantCode, "message-sender-" + suffix.substring(0, 12));
        recipientOne = login(tenantCode, "message-user1-" + suffix.substring(0, 12));
        recipientTwo = login(tenantCode, "message-user2-" + suffix.substring(0, 12));
    }

    @BeforeEach
    void clearOnlyThisFixtureMessages() {
        cleanupMessages();
    }

    @AfterEach
    void removeMessagesCreatedByThisCase() {
        cleanupMessages();
    }

    @AfterAll
    void logoutAndCleanFixtures() {
        try {
            if (sender != null) sender.logout();
            if (recipientOne != null) recipientOne.logout();
            if (recipientTwo != null) recipientTwo.logout();
        } finally {
            cleanupFixtures();
        }
    }

    @Test
    void sendsSeparateInboxRowsAndEnforcesPerRecipientReadAndDeleteOwnership() {
        ResponseEntity<JsonNode> sent = sender.post("/api/system/message/send", sendBody(
                source, "two-recipient-notice", List.of(RECIPIENT_ONE_ID, RECIPIENT_TWO_ID)));
        assertSuccess(sent);
        assertThat(sent.getBody().path("data").asInt()).isEqualTo(2);
        assertThat(messageCount(source)).isEqualTo(2);

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id,tenant_id,user_id,sender_id,read_status FROM sys_message WHERE source=? ORDER BY user_id",
                source);
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(row -> ((Number) row.get("tenant_id")).longValue())
                .containsOnly(TENANT_ID);
        assertThat(rows).extracting(row -> ((Number) row.get("user_id")).longValue())
                .containsExactly(RECIPIENT_ONE_ID, RECIPIENT_TWO_ID);
        assertThat(rows).extracting(row -> ((Number) row.get("sender_id")).longValue())
                .containsOnly(SENDER_ID);
        assertThat(rows).extracting(row -> ((Number) row.get("read_status")).intValue())
                .containsOnly(0);

        JsonNode firstInbox = inbox(recipientOne);
        JsonNode secondInbox = inbox(recipientTwo);
        long firstId = messageId(firstInbox, 0);
        long secondId = messageId(secondInbox, 0);
        assertThat(firstInbox.size()).isEqualTo(1);
        assertThat(secondInbox.size()).isEqualTo(1);
        assertThat(unreadCount(recipientOne)).isEqualTo(1L);
        assertThat(unreadCount(recipientTwo)).isEqualTo(1L);
        assertSuccess(recipientOne.get("/api/system/message/" + firstId));

        assertRejected(recipientOne.get("/api/system/message/" + secondId));
        assertRejected(recipientOne.put("/api/system/message/" + secondId + "/read", null));
        assertRejected(recipientOne.delete("/api/system/message/" + secondId));
        assertThat(messageReadStatus(secondId)).isZero();
        assertThat(messageCount(source)).isEqualTo(2);

        assertSuccess(recipientOne.put("/api/system/message/" + firstId + "/read", null));
        assertSuccess(recipientOne.put("/api/system/message/" + firstId + "/read", null));
        assertThat(messageReadStatus(firstId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT read_time IS NOT NULL FROM sys_message WHERE id=?", Boolean.class, firstId))
                .isTrue();
        assertThat(unreadCount(recipientOne)).isZero();

        ResponseEntity<JsonNode> firstMarkAll = recipientTwo.put("/api/system/message/read-all", null);
        assertSuccess(firstMarkAll);
        assertThat(firstMarkAll.getBody().path("data").asInt()).isEqualTo(1);
        ResponseEntity<JsonNode> secondMarkAll = recipientTwo.put("/api/system/message/read-all", null);
        assertSuccess(secondMarkAll);
        assertThat(secondMarkAll.getBody().path("data").asInt()).isZero();
        assertThat(unreadCount(recipientTwo)).isZero();

        assertSuccess(recipientOne.delete("/api/system/message/" + firstId));
        assertThat(messageExists(firstId)).isFalse();
        assertThat(messageExists(secondId)).isTrue();
        assertSuccess(recipientTwo.delete("/api/system/message/" + secondId));
        assertThat(messageCount(source)).isZero();
    }

    @Test
    void rejectsCrossTenantMessagesAndMixedTenantRecipientBatchesWithoutPartialRows() {
        int foreignCount = sendDirect(FOREIGN_TENANT_ID, FOREIGN_SENDER_ID,
                messageRequest(source + "-foreign", "foreign-tenant-message", List.of(FOREIGN_RECIPIENT_ID)));
        assertThat(foreignCount).isEqualTo(1);
        long foreignMessageId = jdbc.queryForObject(
                "SELECT id FROM sys_message WHERE source=? AND user_id=?", Long.class,
                source + "-foreign", FOREIGN_RECIPIENT_ID);
        assertThat(unreadCount(recipientOne)).isZero();

        assertRejected(recipientOne.get("/api/system/message/" + foreignMessageId));
        assertRejected(recipientOne.put("/api/system/message/" + foreignMessageId + "/read", null));
        assertRejected(recipientOne.delete("/api/system/message/" + foreignMessageId));
        assertThat(messageReadStatus(foreignMessageId)).isZero();
        assertThat(messageExists(foreignMessageId)).isTrue();
        assertThat(unreadCount(recipientOne)).isZero();

        assertThat(messageCount(source)).isZero();
        ResponseEntity<JsonNode> mixedBatch = sender.post("/api/system/message/send", sendBody(
                source, "mixed-tenant-recipients", List.of(RECIPIENT_ONE_ID, FOREIGN_RECIPIENT_ID)));
        assertRejected(mixedBatch);
        assertThat(messageCount(source)).isZero();
        assertThat(messageReadStatus(foreignMessageId)).isZero();
        assertThat(messageExists(foreignMessageId)).isTrue();
    }

    @Test
    void publishesOnlyAfterCommitAndRollsBackRowsAndNotificationsOnRollbackOrInsertFailure() throws Exception {
        String committedSource = source + "-commit";
        String rolledBackSource = source + "-rollback";
        String failedSource = source + "-fail";

        try (RedisProbe probe = new RedisProbe()) {
            probe.startAndAwaitReadiness();
            probe.events.clear();

            transactions.executeWithoutResult(status -> TenantContextHolder.runWithTenantId(TENANT_ID, () ->
                    DataScopeContext.withoutFilter(() -> {
                        int created = messageService.send(SENDER_ID, messageRequest(
                                committedSource, "commit-notice", List.of(RECIPIENT_ONE_ID, RECIPIENT_TWO_ID)));
                        assertThat(created).isEqualTo(2);
                        assertThat(messageCount(committedSource)).isEqualTo(2);
                        pause(Duration.ofMillis(300));
                        assertThat(probe.events).isEmpty();
                        return null;
                    })));

            assertThat(messageCount(committedSource)).isEqualTo(2);
            probe.awaitUserMessages(2);
            assertThat(probe.events).extracting(RedisRealtimePublisher.Envelope::kind)
                    .containsOnly("user");
            assertThat(probe.events).extracting(RedisRealtimePublisher.Envelope::tenant)
                    .containsOnly(TENANT_ID);
            assertThat(probe.events).extracting(RedisRealtimePublisher.Envelope::user)
                    .containsExactlyInAnyOrder(RECIPIENT_ONE_ID, RECIPIENT_TWO_ID);
            assertThat(probe.events).extracting(event -> event.message().type())
                    .containsOnly("system-message");

            probe.events.clear();
            long committedRowCount = messageCount(committedSource);
            transactions.executeWithoutResult(status -> TenantContextHolder.runWithTenantId(TENANT_ID, () ->
                    DataScopeContext.withoutFilter(() -> {
                        int created = messageService.send(SENDER_ID, messageRequest(
                                rolledBackSource, "rollback-notice", List.of(RECIPIENT_ONE_ID, RECIPIENT_TWO_ID)));
                        assertThat(created).isEqualTo(2);
                        assertThat(messageCount(rolledBackSource)).isEqualTo(2);
                        pause(Duration.ofMillis(300));
                        assertThat(probe.events).isEmpty();
                        status.setRollbackOnly();
                        return null;
                    })));

            assertThat(messageCount(rolledBackSource)).isZero();
            assertThat(messageCount(committedSource)).isEqualTo(committedRowCount);
            pause(Duration.ofMillis(300));
            assertThat(probe.events).isEmpty();

            try {
                createInsertFailureTrigger(failedSource);
                ResponseEntity<JsonNode> failed = sender.post("/api/system/message/send", sendBody(
                        failedSource, "second-row-failure", List.of(RECIPIENT_ONE_ID, RECIPIENT_TWO_ID)));
                assertRejected(failed);
                assertThat(messageCount(failedSource)).isZero();
                pause(Duration.ofMillis(300));
                assertThat(probe.events).isEmpty();
            } finally {
                dropInsertFailureTrigger();
            }
        }
    }

    private void seedFixtures() {
        jdbc.update("INSERT INTO sys_tenant_package(id,package_name,status,remark) VALUES(?,?,0,?)",
                PACKAGE_ID, "message-package-" + suffix, "message lifecycle fixture");
        jdbc.update("INSERT INTO sys_tenant_package_menu(package_id,menu_id) "
                        + "SELECT ?, id FROM sys_menu WHERE perm IN (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
                new Object[]{PACKAGE_ID, MESSAGE_PERMISSIONS.get(0), MESSAGE_PERMISSIONS.get(1),
                        MESSAGE_PERMISSIONS.get(2), MESSAGE_PERMISSIONS.get(3), MESSAGE_PERMISSIONS.get(4)});
        jdbc.update("INSERT INTO sys_tenant(id,tenant_code,tenant_name,package_id,account_count,status,auth_version) "
                        + "VALUES(?,?,?, ?,100,0,0)",
                TENANT_ID, tenantCode, "Message Lifecycle Tenant", PACKAGE_ID);
        jdbc.update("INSERT INTO sys_tenant(id,tenant_code,tenant_name,package_id,account_count,status,auth_version) "
                        + "VALUES(?,?,?, ?,100,0,0)",
                FOREIGN_TENANT_ID, foreignTenantCode, "Message Lifecycle Other Tenant", PACKAGE_ID);
        jdbc.update("INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope) VALUES(?,?,?, ?,0,0,1)",
                ROLE_ID, TENANT_ID, "Message Lifecycle Role", "message-role-" + suffix.substring(0, 12));
        jdbc.update("INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope) VALUES(?,?,?, ?,0,0,1)",
                FOREIGN_ROLE_ID, FOREIGN_TENANT_ID, "Message Lifecycle Foreign Role", "message-other-" + suffix.substring(0, 8));
        insertUser(SENDER_ID, TENANT_ID, "message-sender-" + suffix.substring(0, 12), "Message Sender");
        insertUser(RECIPIENT_ONE_ID, TENANT_ID, "message-user1-" + suffix.substring(0, 12), "Message Receiver One");
        insertUser(RECIPIENT_TWO_ID, TENANT_ID, "message-user2-" + suffix.substring(0, 12), "Message Receiver Two");
        insertUser(FOREIGN_SENDER_ID, FOREIGN_TENANT_ID,
                "message-other-sender-" + suffix.substring(0, 8), "Other Tenant Sender");
        insertUser(FOREIGN_RECIPIENT_ID, FOREIGN_TENANT_ID,
                "message-other-user-" + suffix.substring(0, 8), "Other Tenant Receiver");
        for (long userId : List.of(SENDER_ID, RECIPIENT_ONE_ID, RECIPIENT_TWO_ID)) {
            jdbc.update("INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES(?,?,?)",
                    TENANT_ID, userId, ROLE_ID);
        }
        for (long userId : List.of(FOREIGN_SENDER_ID, FOREIGN_RECIPIENT_ID)) {
            jdbc.update("INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES(?,?,?)",
                    FOREIGN_TENANT_ID, userId, FOREIGN_ROLE_ID);
        }
        jdbc.update("INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) "
                        + "SELECT ?, ?, id FROM sys_menu WHERE perm IN (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
                new Object[]{TENANT_ID, ROLE_ID, MESSAGE_PERMISSIONS.get(0), MESSAGE_PERMISSIONS.get(1),
                        MESSAGE_PERMISSIONS.get(2), MESSAGE_PERMISSIONS.get(3), MESSAGE_PERMISSIONS.get(4)});
    }

    private void insertUser(long userId, long tenantId, String username, String nickname) {
        jdbc.update("INSERT INTO sys_user(id,tenant_id,dept_id,username,password,nickname,status,auth_version) "
                        + "VALUES(?,?,NULL,?,?,?,0,0)",
                userId, tenantId, username, BCrypt.hashpw(PASSWORD), nickname);
    }

    private void cleanupFixtures() {
        if (jdbc == null) return;
        cleanupMessages();
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id IN (?,?)", ROLE_ID, FOREIGN_ROLE_ID);
        jdbc.update("DELETE FROM sys_user_role WHERE user_id IN (?,?,?,?,?) OR role_id IN (?,?)",
                SENDER_ID, RECIPIENT_ONE_ID, RECIPIENT_TWO_ID, FOREIGN_SENDER_ID, FOREIGN_RECIPIENT_ID,
                ROLE_ID, FOREIGN_ROLE_ID);
        jdbc.update("DELETE FROM sys_user WHERE id IN (?,?,?,?,?)",
                SENDER_ID, RECIPIENT_ONE_ID, RECIPIENT_TWO_ID, FOREIGN_SENDER_ID, FOREIGN_RECIPIENT_ID);
        jdbc.update("DELETE FROM sys_role WHERE id IN (?,?)", ROLE_ID, FOREIGN_ROLE_ID);
        jdbc.update("DELETE FROM sys_tenant WHERE id IN (?,?)", TENANT_ID, FOREIGN_TENANT_ID);
        jdbc.update("DELETE FROM sys_tenant_package_menu WHERE package_id=?", PACKAGE_ID);
        jdbc.update("DELETE FROM sys_tenant_package WHERE id=?", PACKAGE_ID);
    }

    private void cleanupMessages() {
        jdbc.update("DELETE FROM sys_message WHERE source=? OR source LIKE ? "
                        + "OR user_id IN (?,?,?,?,?) OR sender_id IN (?,?,?)",
                source, source + "-%", SENDER_ID, RECIPIENT_ONE_ID, RECIPIENT_TWO_ID,
                FOREIGN_SENDER_ID, FOREIGN_RECIPIENT_ID,
                SENDER_ID, FOREIGN_SENDER_ID, RECIPIENT_ONE_ID);
    }

    private Session login(String tenant, String username) {
        long waitMillis = 1050 - (System.currentTimeMillis() - previousLoginAt);
        if (previousLoginAt != 0 && waitMillis > 0) pause(Duration.ofMillis(waitMillis));
        ResponseEntity<JsonNode> response = rest.postForEntity("/api/auth/login", Map.of(
                "tenantCode", tenant, "clientId", "nz-web-account",
                "username", username, "password", PASSWORD), JsonNode.class);
        previousLoginAt = System.currentTimeMillis();
        assertSuccess(response);
        String token = response.getBody().path("data").asText();
        assertThat(token).isNotBlank();
        return new Session(token);
    }

    private Map<String, Object> sendBody(String messageSource, String title, List<Long> userIds) {
        Map<String, Object> body = new HashMap<>();
        body.put("category", "notice");
        body.put("type", "notice");
        body.put("source", messageSource);
        body.put("title", title);
        body.put("summary", "Lifecycle summary");
        body.put("content", "Message lifecycle test content");
        body.put("dataJson", "{\"source\":\"" + messageSource + "\"}");
        body.put("path", "/system/message");
        body.put("targetType", "USERS");
        body.put("userIds", userIds);
        return body;
    }

    private MessageSendRequest messageRequest(String messageSource, String title, List<Long> userIds) {
        return new MessageSendRequest(
                "notice", "notice", messageSource, title, "Lifecycle summary",
                "Message lifecycle test content", "{\"source\":\"" + messageSource + "\"}",
                "/system/message", "USERS", userIds);
    }

    private int sendDirect(long tenantId, long senderId, MessageSendRequest request) {
        return TenantContextHolder.callWithTenantId(tenantId,
                () -> DataScopeContext.withoutFilter(() -> messageService.send(senderId, request)));
    }

    private JsonNode inbox(Session session) {
        return success(session.get("/api/system/message/page?pageNum=1&pageSize=20"))
                .path("data").path("records");
    }

    private long unreadCount(Session session) {
        return success(session.get("/api/system/message/unread-count")).path("data").asLong();
    }

    private long messageId(JsonNode records, int index) {
        return records.path(index).path("id").asLong();
    }

    private int messageReadStatus(long messageId) {
        return jdbc.queryForObject("SELECT read_status FROM sys_message WHERE id=?", Integer.class, messageId);
    }

    private boolean messageExists(long messageId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM sys_message WHERE id=?)", Boolean.class, messageId));
    }

    private long messageCount(String messageSource) {
        return jdbc.queryForObject("SELECT count(*) FROM sys_message WHERE source=?", Long.class, messageSource);
    }

    private void createInsertFailureTrigger(String messageSource) {
        String functionName = "test_message_insert_failure_" + suffix.substring(0, 12);
        String triggerName = "test_message_insert_failure_trigger_" + suffix.substring(0, 12);
        jdbc.execute("CREATE OR REPLACE FUNCTION " + functionName + "() RETURNS trigger LANGUAGE plpgsql AS $$ "
                + "BEGIN IF NEW.source = '" + messageSource + "' AND NEW.user_id = " + RECIPIENT_TWO_ID
                + " THEN RAISE EXCEPTION 'message lifecycle injected insert failure'; END IF; "
                + "RETURN NEW; END; $$");
        jdbc.execute("CREATE TRIGGER " + triggerName + " BEFORE INSERT ON sys_message "
                + "FOR EACH ROW EXECUTE FUNCTION " + functionName + "()");
    }

    private void dropInsertFailureTrigger() {
        String functionName = "test_message_insert_failure_" + suffix.substring(0, 12);
        String triggerName = "test_message_insert_failure_trigger_" + suffix.substring(0, 12);
        jdbc.execute("DROP TRIGGER IF EXISTS " + triggerName + " ON sys_message");
        jdbc.execute("DROP FUNCTION IF EXISTS " + functionName + "()");
    }

    private void assertSuccess(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asInt())
                .as(response.getBody().path("msg").asText()).isEqualTo(200);
    }

    private JsonNode success(ResponseEntity<JsonNode> response) {
        assertSuccess(response);
        return response.getBody();
    }

    private void assertRejected(ResponseEntity<JsonNode> response) {
        boolean rejected = !response.getStatusCode().is2xxSuccessful()
                || (response.getBody() != null && response.getBody().path("code").asInt() != 200);
        assertThat(rejected)
                .as("expected message operation to be rejected, got HTTP %s body %s",
                        response.getStatusCode(), response.getBody())
                .isTrue();
    }

    private void pause(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("消息生命周期测试等待被中断", exception);
        }
    }

    private final class Session {
        private final String token;

        private Session(String token) {
            this.token = token;
        }

        private ResponseEntity<JsonNode> get(String path) {
            return exchange(HttpMethod.GET, path, null);
        }

        private ResponseEntity<JsonNode> post(String path, Object body) {
            return exchange(HttpMethod.POST, path, body);
        }

        private ResponseEntity<JsonNode> put(String path, Object body) {
            return exchange(HttpMethod.PUT, path, body);
        }

        private ResponseEntity<JsonNode> delete(String path) {
            return exchange(HttpMethod.DELETE, path, null);
        }

        private ResponseEntity<JsonNode> exchange(HttpMethod method, String path, Object body) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            return rest.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
        }

        private void logout() {
            exchange(HttpMethod.POST, "/api/auth/logout", null);
        }
    }

    private final class RedisProbe implements AutoCloseable, MessageListener {
        private final LettuceConnectionFactory factory;
        private final RedisMessageListenerContainer listener;
        private final CountDownLatch ready = new CountDownLatch(1);
        private final AtomicReference<IOException> listenerFailure = new AtomicReference<>();
        private final List<RedisRealtimePublisher.Envelope> events = new CopyOnWriteArrayList<>();

        private RedisProbe() {
            int port = Integer.parseInt(System.getenv("NZ_TEST_REDIS_PORT"));
            factory = new LettuceConnectionFactory("127.0.0.1", port);
            factory.afterPropertiesSet();
            listener = new RedisMessageListenerContainer();
            listener.setConnectionFactory(factory);
            listener.addMessageListener(this, new ChannelTopic(channel));
        }

        private void startAndAwaitReadiness() throws Exception {
            listener.afterPropertiesSet();
            listener.start();
            RealtimeMessage probeMessage = RealtimeMessage.of(
                    "message-lifecycle-probe", Map.of("fixture", suffix));
            RedisRealtimePublisher.Envelope probe = new RedisRealtimePublisher.Envelope(
                    "probe-" + suffix, "user", TENANT_ID, RECIPIENT_ONE_ID, probeMessage);
            String encoded = mapper.writeValueAsString(probe);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (ready.getCount() > 0 && System.nanoTime() < deadline) {
                redis.convertAndSend(channel, encoded);
                ready.await(100, TimeUnit.MILLISECONDS);
            }
            assertThat(ready.getCount()).as("Redis realtime channel subscription probe").isZero();
            assertThat(listenerFailure.get()).isNull();
        }

        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                RedisRealtimePublisher.Envelope envelope = mapper.readValue(
                        new String(message.getBody(), StandardCharsets.UTF_8),
                        RedisRealtimePublisher.Envelope.class);
                if ("message-lifecycle-probe".equals(envelope.message().type())) {
                    ready.countDown();
                    return;
                }
                events.add(envelope);
            } catch (IOException exception) {
                listenerFailure.compareAndSet(null, exception);
                ready.countDown();
            }
        }

        private void awaitUserMessages(int count) {
            org.awaitility.Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> assertThat(events).hasSize(count));
            assertThat(listenerFailure.get()).isNull();
        }

        @Override
        public void close() throws Exception {
            listener.stop();
            listener.destroy();
            factory.destroy();
        }
    }
}
