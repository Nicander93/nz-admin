package com.nz.admin;

import cn.hutool.crypto.digest.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.framework.auth.core.AuthSessionIdentity;
import com.fasterxml.jackson.databind.JsonNode;
import com.nz.admin.framework.cache.core.AtomicStateStore;
import com.nz.admin.framework.datascope.core.DataScopeContext;
import com.nz.admin.framework.realtime.web.RealtimeEndpoints;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.service.tenant.TenantService;
import com.nz.admin.modules.system.service.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 真实 PostgreSQL 身份版本、HTTP 会话与实时连接生命周期回归。 */
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
class IdentityLifecyclePostgresTest {

    private static final String CACHE_KEY_PREFIX = "identity-lifecycle-" + UUID.randomUUID();
    private static final long TEST_TENANT_ID = 980100L;
    private static final long TEST_USER_ID = 980101L;
    private static final long TENANT_USER_ID = 980102L;
    private static final long MANAGER_ROLE_ID = 980110L;
    private static final long OWNER_ROLE_ID = 980120L;
    private static final long REALTIME_ROLE_ID = 980121L;
    private static final long TEST_PACKAGE_ID = 980130L;
    private static final String PASSWORD = "identity-password";
    private static final String RESET_PASSWORD_FALLBACK = "123456";
    private static final List<String> MANAGER_PERMISSIONS = List.of(
            "system:user:edit", "system:user:resetPwd", "system:user:remove",
            "system:tenant:edit", "system:tenant:remove",
            "system:realtime:view", "system:realtime:send");
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired AtomicStateStore states;
    @Autowired TransactionTemplate transactions;
    @Autowired UserService userService;
    @Autowired TenantService tenantService;
    @LocalServerPort
    int port;

    private final List<AutoCloseable> connections = new CopyOnWriteArrayList<>();
    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private String managerName;
    private String targetName;
    private String tenantOwnerName;
    private String tenantCode;
    private String targetPhone;
    private String resetPassword;
    private long previousLoginAt;

    @DynamicPropertySource
    static void isolateCacheKeys(DynamicPropertyRegistry registry) {
        registry.add("nz.cache.key-prefix", () -> CACHE_KEY_PREFIX);
    }

    @BeforeEach
    void seedFixturesAndClearOnlyThisLoginWindow() {
        clearLoginWindow();
        cleanupFixtures();
        managerName = "identity-manager-" + suffix;
        targetName = "identity-user-" + suffix;
        tenantOwnerName = "identity-owner-" + suffix;
        tenantCode = "identity-" + suffix;
        targetPhone = "138" + String.format("%08d", Math.floorMod(suffix.hashCode(), 100_000_000));
        seedFixtures();
    }

    @AfterEach
    void cleanupFixturesAndConnections() {
        for (AutoCloseable connection : connections) {
            try {
                connection.close();
            } catch (Exception ignored) {
                // Connection cleanup is best-effort after lifecycle assertions finish.
            }
        }
        connections.clear();
        cleanupFixtures();
    }

    @Test
    void aSessionWithoutVersionMetadataRequiresLoginAfterUpgrade() {
        Session old = login("default", targetName, PASSWORD);
        var serverSession = StpUtil.getStpLogic().getTokenSessionByToken(old.token(), false);
        serverSession.delete(AuthSessionIdentity.USER_VERSION);
        serverSession.delete(AuthSessionIdentity.TENANT_VERSION);

        assertUnauthorized(old.get("/api/auth/info"));
        Session fresh = login("default", targetName, PASSWORD);
        assertThat(fresh.token()).isNotEqualTo(old.token());
        assertSuccess(fresh.get("/api/auth/info"));
        assertUnauthorized(old.get("/api/auth/info"));
    }

    @Test
    void disablingAndReenablingUserDoesNotReviveOldToken() {
        Session manager = login("default", managerName, PASSWORD);
        Session old = login("default", targetName, PASSWORD);
        Session oldSecond = login("default", targetName, PASSWORD);

        assertSuccess(old.get("/api/auth/info"));
        assertSuccess(oldSecond.get("/api/auth/info"));
        assertSuccess(manager.put("/api/system/user", Map.of("id", TEST_USER_ID, "status", 1)));
        assertUnauthorized(old.get("/api/auth/info"));
        assertUnauthorized(oldSecond.get("/api/auth/info"));

        assertSuccess(manager.put("/api/system/user", Map.of("id", TEST_USER_ID, "status", 0)));
        assertUnauthorized(old.get("/api/auth/info"));
        assertUnauthorized(oldSecond.get("/api/auth/info"));
        Session fresh = login("default", targetName, PASSWORD);
        assertThat(fresh.token()).isNotEqualTo(old.token());
        assertSuccess(fresh.get("/api/auth/info"));
    }

    @Test
    void ordinaryProfileEditKeepsTheCurrentTokenValid() {
        Session user = login("default", targetName, PASSWORD);
        Session second = login("default", targetName, PASSWORD);
        long versionBefore = userVersion(TEST_USER_ID);

        assertSuccess(user.put("/api/system/profile", Map.of(
                "nickname", "Updated identity profile", "email", "profile-" + suffix + "@example.test",
                "phone", targetPhone, "gender", "2")));

        assertThat(userVersion(TEST_USER_ID)).isEqualTo(versionBefore);
        assertSuccess(user.get("/api/auth/info"));
        assertSuccess(second.get("/api/auth/info"));
    }

    @Test
    void clearingPhoneInvalidatesOnceAndRepeatingTheClearKeepsTheNewToken() {
        Session old = login("default", targetName, PASSWORD);
        assertThat(jdbc.queryForObject("SELECT phone FROM sys_user WHERE id=?", String.class, TEST_USER_ID))
                .isNotBlank();
        long before = userVersion(TEST_USER_ID);

        assertSuccess(old.put("/api/system/profile", Map.of(
                "nickname", "Identity User", "email", "", "phone", "", "gender", "2")));
        assertUnauthorized(old.get("/api/auth/info"));
        assertThat(userVersion(TEST_USER_ID)).isEqualTo(before + 1);

        Session current = login("default", targetName, PASSWORD);
        long afterFirstClear = userVersion(TEST_USER_ID);
        assertSuccess(current.put("/api/system/profile", Map.of(
                "nickname", "Identity User", "email", "", "phone", "", "gender", "2")));
        assertThat(userVersion(TEST_USER_ID)).isEqualTo(afterFirstClear);
        assertSuccess(current.get("/api/auth/info"));
    }

    @Test
    void passwordChangeRejectsWrongOldPasswordAndInvalidatesEveryEarlierToken() {
        Session first = login("default", targetName, PASSWORD);
        Session second = login("default", targetName, PASSWORD);
        assertThat(first.token()).isNotEqualTo(second.token());

        assertBusinessFailure(first.put("/api/system/profile/password",
                Map.of("oldPassword", "wrong-password", "newPassword", "identity-new-password")));
        assertSuccess(first.get("/api/auth/info"));
        assertSuccess(second.get("/api/auth/info"));

        assertSuccess(first.put("/api/system/profile/password",
                Map.of("oldPassword", PASSWORD, "newPassword", "identity-new-password")));
        assertUnauthorized(first.get("/api/auth/info"));
        assertUnauthorized(second.get("/api/auth/info"));

        Session fresh = login("default", targetName, "identity-new-password");
        assertThat(fresh.token()).isNotEqualTo(first.token()).isNotEqualTo(second.token());
        assertSuccess(fresh.get("/api/auth/info"));
        assertUnauthorized(first.get("/api/auth/info"));
    }

    @Test
    void administratorPasswordResetInvalidatesOldTokenAndAllowsNewPasswordLogin() {
        Session manager = login("default", managerName, PASSWORD);
        Session old = login("default", targetName, PASSWORD);
        Session oldSecond = login("default", targetName, PASSWORD);

        assertSuccess(manager.put("/api/system/user/" + TEST_USER_ID + "/password/reset", null));
        assertUnauthorized(old.get("/api/auth/info"));
        assertUnauthorized(oldSecond.get("/api/auth/info"));

        Session reset = login("default", targetName, resetPassword);
        assertThat(reset.token()).isNotEqualTo(old.token()).isNotEqualTo(oldSecond.token());
        assertSuccess(reset.get("/api/auth/info"));
        assertUnauthorized(old.get("/api/auth/info"));
        assertUnauthorized(oldSecond.get("/api/auth/info"));
    }

    @Test
    void deletingUserMakesItsExistingTokenInvalid() {
        Session manager = login("default", managerName, PASSWORD);
        Session old = login("default", targetName, PASSWORD);
        Session oldSecond = login("default", targetName, PASSWORD);

        assertSuccess(manager.delete("/api/system/user/" + TEST_USER_ID));
        assertUnauthorized(old.get("/api/auth/info"));
        assertUnauthorized(oldSecond.get("/api/auth/info"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sys_user WHERE id=?", Integer.class, TEST_USER_ID))
                .isZero();
    }

    @Test
    void tenantStatusExpiryAndPackageChangesInvalidatePreviousVersions() {
        Session manager = login("default", managerName, PASSWORD);
        Session original = login(tenantCode, tenantOwnerName, PASSWORD);

        assertSuccess(manager.delete("/api/system/tenant/" + TEST_TENANT_ID));
        assertUnauthorized(original.get("/api/auth/info"));
        assertSuccess(manager.put("/api/system/tenant", tenantUpdate(0, null, 1L)));
        assertUnauthorized(original.get("/api/auth/info"));

        Session reenabled = login(tenantCode, tenantOwnerName, PASSWORD);
        assertSuccess(reenabled.get("/api/auth/info"));
        assertSuccess(manager.put("/api/system/tenant", tenantUpdate(0, LocalDateTime.now().minusDays(1), 1L)));
        assertUnauthorized(reenabled.get("/api/auth/info"));
        assertSuccess(manager.put("/api/system/tenant", tenantUpdate(0, LocalDateTime.now().plusDays(30), 1L)));
        assertUnauthorized(reenabled.get("/api/auth/info"));

        Session renewed = login(tenantCode, tenantOwnerName, PASSWORD);
        assertSuccess(renewed.get("/api/auth/info"));
        assertSuccess(manager.put("/api/system/tenant", tenantUpdate(0, LocalDateTime.now().plusDays(30), TEST_PACKAGE_ID)));
        assertUnauthorized(renewed.get("/api/auth/info"));

        Session repackaged = login(tenantCode, tenantOwnerName, PASSWORD);
        assertThat(repackaged.token()).isNotEqualTo(renewed.token());
        assertSuccess(repackaged.get("/api/auth/info"));
        assertThat(tenantVersion(TEST_TENANT_ID)).isEqualTo(5L);
    }

    @Test
    void tenantExpiresNaturallyWithoutAnotherVersionChange() {
        Session manager = login("default", managerName, PASSWORD);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(3);
        assertSuccess(manager.put("/api/system/tenant", tenantUpdate(0, expiresAt, 1L)));
        long versionAtExpiry = tenantVersion(TEST_TENANT_ID);
        Session expiring = login(tenantCode, tenantOwnerName, PASSWORD);
        assertSuccess(expiring.get("/api/auth/info"));

        org.awaitility.Awaitility.await()
                .pollInterval(Duration.ofMillis(50))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertUnauthorized(expiring.get("/api/auth/info")));

        assertThat(tenantVersion(TEST_TENANT_ID)).isEqualTo(versionAtExpiry);
        assertSuccess(manager.put("/api/system/tenant",
                tenantUpdate(0, LocalDateTime.now().plusDays(30), 1L)));
        assertUnauthorized(expiring.get("/api/auth/info"));
        Session renewed = login(tenantCode, tenantOwnerName, PASSWORD);
        assertSuccess(renewed.get("/api/auth/info"));
    }

    @Test
    void transactionRollbackRestoresIdentityVersionsAndKeepsExistingTokenUsable() {
        Session user = login("default", targetName, PASSWORD);
        Session second = login("default", targetName, PASSWORD);
        long userVersionBefore = userVersion(TEST_USER_ID);
        long tenantVersionBefore = tenantVersion(TEST_TENANT_ID);

        transactions.executeWithoutResult(status -> {
            updateFixtureUser(new UserDO().setId(TEST_USER_ID).setStatus(1));
            tenantService.deactivate(TEST_TENANT_ID);
            status.setRollbackOnly();
        });

        assertThat(userVersion(TEST_USER_ID)).isEqualTo(userVersionBefore);
        assertThat(tenantVersion(TEST_TENANT_ID)).isEqualTo(tenantVersionBefore);
        assertThat(jdbc.queryForObject("SELECT status FROM sys_user WHERE id=?", Integer.class, TEST_USER_ID))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM sys_tenant WHERE id=?", Integer.class, TEST_TENANT_ID))
                .isZero();
        assertSuccess(user.get("/api/auth/info"));
        assertSuccess(second.get("/api/auth/info"));
    }

    @Test
    void staleIdentityClosesConnectedSseAndWebSocketAndRejectsPendingTickets() throws Exception {
        Session manager = login("default", managerName, PASSWORD);
        Session old = login("default", targetName, PASSWORD);
        String connectedSseTicket = issueTicket(old, "SSE");
        String connectedWsTicket = issueTicket(old, "WEBSOCKET");
        String pendingSseTicket = issueTicket(old, "SSE");
        String pendingWsTicket = issueTicket(old, "WEBSOCKET");
        SseConnection oldSse = connectSse(connectedSseTicket);
        connections.add(oldSse);
        SocketConnection oldSocket = connectWebSocket(connectedWsTicket);
        connections.add(oldSocket);
        oldSse.awaitEvent("connected");
        oldSocket.awaitMessage("connected");

        assertSuccess(manager.put("/api/system/user", Map.of("id", TEST_USER_ID, "status", 1)));
        assertUnauthorized(old.get("/api/auth/info"));
        oldSse.awaitClosed();
        oldSocket.awaitClosed();
        HttpResponse<InputStream> pendingSse = openTicketSse(pendingSseTicket);
        assertThat(pendingSse.statusCode()).isEqualTo(401);
        pendingSse.body().close();
        assertThatThrownBy(() -> connectWebSocket(pendingWsTicket))
                .isInstanceOf(Exception.class);

        assertSuccess(manager.put("/api/system/user", Map.of("id", TEST_USER_ID, "status", 0)));
        Session fresh = login("default", targetName, PASSWORD);
        String freshSseTicket = issueTicket(fresh, "SSE");
        String freshWsTicket = issueTicket(fresh, "WEBSOCKET");
        SseConnection freshSse = connectSse(freshSseTicket);
        connections.add(freshSse);
        SocketConnection freshSocket = connectWebSocket(freshWsTicket);
        connections.add(freshSocket);
        freshSse.awaitEvent("connected");
        freshSocket.awaitMessage("connected");
        Thread.sleep(1500);
        assertThat(freshSocket.isOpen()).isTrue();

        assertThat(fresh.post("/api/system/realtime/test", Map.of("message", "new-version-live")).getBody()
                .path("data").asInt()).isEqualTo(2);
        freshSse.awaitEvent("new-version-live");
        freshSocket.awaitMessage("new-version-live");
    }

    /** 测试准备与回滚注入使用明确的可信上下文，HTTP 操作仍走完整鉴权。 */
    private void updateFixtureUser(UserDO user) {
        TenantContextHolder.runWithTenantId(1L, () -> DataScopeContext.withoutFilter(() -> {
            userService.updateById(user);
            return null;
        }));
    }

    private void seedFixtures() {
        jdbc.update("INSERT INTO sys_tenant_package(id,package_name,status,remark) VALUES(?,?,0,'identity lifecycle') "
                        + "ON CONFLICT(id) DO UPDATE SET package_name=EXCLUDED.package_name,status=0",
                TEST_PACKAGE_ID, "identity-package-" + suffix);
        jdbc.update("INSERT INTO sys_tenant(id,tenant_code,tenant_name,package_id,account_count,status,auth_version) "
                        + "VALUES(?,?,?,1,100,0,0) ON CONFLICT(id) DO UPDATE SET tenant_code=EXCLUDED.tenant_code,"
                        + "tenant_name=EXCLUDED.tenant_name,package_id=1,account_count=100,status=0,expire_time=NULL,auth_version=0",
                TEST_TENANT_ID, tenantCode, "Identity Lifecycle Tenant");

        jdbc.update("INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope) VALUES(?,1,?,?,0,0,1) "
                        + "ON CONFLICT(id) DO UPDATE SET tenant_id=1,name=EXCLUDED.name,role_key=EXCLUDED.role_key,status=0,data_scope=1",
                MANAGER_ROLE_ID, "Identity Lifecycle Manager", "identity-manager-" + suffix);
        jdbc.update("INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope) VALUES(?,?,?,'identity-owner',0,0,1) "
                        + "ON CONFLICT(id) DO UPDATE SET tenant_id=EXCLUDED.tenant_id,name=EXCLUDED.name,status=0,data_scope=1",
                OWNER_ROLE_ID, TEST_TENANT_ID, "Identity Lifecycle Owner");
        jdbc.update("INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope) VALUES(?,1,'Identity Realtime User','identity-realtime',0,0,1) "
                        + "ON CONFLICT(id) DO UPDATE SET tenant_id=1,name=EXCLUDED.name,status=0,data_scope=1",
                REALTIME_ROLE_ID);

        jdbc.update("INSERT INTO sys_user(id,tenant_id,dept_id,username,password,nickname,status,auth_version) "
                        + "VALUES(?,1,NULL,?,?,?,0,0) ON CONFLICT(id) DO UPDATE SET tenant_id=1,dept_id=NULL,"
                        + "username=EXCLUDED.username,password=EXCLUDED.password,nickname=EXCLUDED.nickname,status=0,auth_version=0",
                980103L, managerName, BCrypt.hashpw(PASSWORD), "Identity Lifecycle Manager");
        jdbc.update("INSERT INTO sys_user(id,tenant_id,dept_id,username,password,nickname,status,auth_version) "
                        + "VALUES(?,1,NULL,?,?,?,0,0) ON CONFLICT(id) DO UPDATE SET tenant_id=1,dept_id=NULL,"
                        + "username=EXCLUDED.username,password=EXCLUDED.password,nickname=EXCLUDED.nickname,status=0,auth_version=0",
                TEST_USER_ID, targetName, BCrypt.hashpw(PASSWORD), "Identity Lifecycle User");
        jdbc.update("INSERT INTO sys_user(id,tenant_id,dept_id,username,password,nickname,status,auth_version) "
                        + "VALUES(?,?,NULL,?,?,?,0,0) ON CONFLICT(id) DO UPDATE SET tenant_id=EXCLUDED.tenant_id,dept_id=NULL,"
                        + "username=EXCLUDED.username,password=EXCLUDED.password,nickname=EXCLUDED.nickname,status=0,auth_version=0",
                TENANT_USER_ID, TEST_TENANT_ID, tenantOwnerName, BCrypt.hashpw(PASSWORD), "Identity Tenant Owner");
        jdbc.update("INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES(1,?,?) ON CONFLICT DO NOTHING",
                980103L, MANAGER_ROLE_ID);
        jdbc.update("INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES(1,?,?) ON CONFLICT DO NOTHING",
                TEST_USER_ID, REALTIME_ROLE_ID);
        jdbc.update("INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES(?,?,?) ON CONFLICT DO NOTHING",
                TEST_TENANT_ID, TENANT_USER_ID, OWNER_ROLE_ID);
        for (String permission : MANAGER_PERMISSIONS) {
            jdbc.update("INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) "
                            + "SELECT 1, ?, id FROM sys_menu WHERE perm=? ON CONFLICT DO NOTHING",
                    MANAGER_ROLE_ID, permission);
        }
        jdbc.update("INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) "
                        + "SELECT 1, ?, id FROM sys_menu WHERE perm IN ('system:realtime:view','system:realtime:send') ON CONFLICT DO NOTHING",
                REALTIME_ROLE_ID);
        updateFixtureUser(new UserDO().setId(TEST_USER_ID).setPhone(targetPhone));

        resetPassword = jdbc.query("SELECT config_value FROM sys_config WHERE tenant_id=1 AND config_key='sys.user.initPassword'",
                rs -> rs.next() ? rs.getString(1) : RESET_PASSWORD_FALLBACK);
        if (resetPassword == null || resetPassword.isBlank()) resetPassword = RESET_PASSWORD_FALLBACK;
    }

    private void cleanupFixtures() {
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id IN (?,?,?)",
                MANAGER_ROLE_ID, OWNER_ROLE_ID, REALTIME_ROLE_ID);
        jdbc.update("DELETE FROM sys_user_role WHERE user_id IN (?,?,?) OR role_id IN (?,?,?)",
                980103L, TEST_USER_ID, TENANT_USER_ID, MANAGER_ROLE_ID, OWNER_ROLE_ID, REALTIME_ROLE_ID);
        jdbc.update("DELETE FROM sys_user WHERE id IN (?,?,?)",
                980103L, TEST_USER_ID, TENANT_USER_ID);
        jdbc.update("DELETE FROM sys_role WHERE id IN (?,?,?)",
                MANAGER_ROLE_ID, OWNER_ROLE_ID, REALTIME_ROLE_ID);
        jdbc.update("DELETE FROM sys_tenant WHERE id=? OR tenant_code=?", TEST_TENANT_ID,
                tenantCode == null ? "identity-" + suffix : tenantCode);
        jdbc.update("DELETE FROM sys_tenant_package_menu WHERE package_id=?", TEST_PACKAGE_ID);
        jdbc.update("DELETE FROM sys_tenant_package WHERE id=?", TEST_PACKAGE_ID);
    }

    private void clearLoginWindow() {
        states.take("rate-limit:POST:/api/auth/login:127.0.0.1");
        states.take("rate-limit:POST:/api/auth/login:0:0:0:0:0:0:0:1");
    }

    private Session login(String tenant, String username, String password) {
        // 两次真实登录遵守接口的一秒防重窗口，跨测试也保留上一登录时间。
        long waitMillis = 1050 - (System.currentTimeMillis() - previousLoginAt);
        if (previousLoginAt != 0 && waitMillis > 0) {
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("登录节流等待被中断", error);
            }
        }
        ResponseEntity<JsonNode> response = rest.postForEntity("/api/auth/login", Map.of(
                "tenantCode", tenant, "clientId", "nz-web-account", "username", username, "password", password), JsonNode.class);
        previousLoginAt = System.currentTimeMillis();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asInt()).as(response.getBody().path("msg").asText()).isEqualTo(200);
        String token = response.getBody().path("data").asText();
        assertThat(token).isNotBlank();
        return new Session(token);
    }

    private ResponseEntity<JsonNode> request(Session session, HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", session.token());
        return rest.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private JsonNode success(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asInt())
                .as(response.getBody().path("msg").asText()).isEqualTo(200);
        return response.getBody();
    }

    private void assertSuccess(ResponseEntity<JsonNode> response) {
        success(response);
    }

    private void assertBusinessFailure(ResponseEntity<JsonNode> response) {
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asInt()).isNotEqualTo(200);
    }

    private void assertUnauthorized(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()
                        || (response.getBody() != null && response.getBody().path("code").asInt() == 401))
                .as("expected identity rejection, received HTTP %s body %s", response.getStatusCode(), response.getBody())
                .isTrue();
    }

    private Map<String, Object> tenantUpdate(Integer status, LocalDateTime expireTime, Long packageId) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", TEST_TENANT_ID);
        request.put("tenantCode", tenantCode);
        request.put("tenantName", "Identity Lifecycle Tenant");
        request.put("contactUser", "Identity Lifecycle Owner");
        request.put("packageId", packageId);
        request.put("expireTime", expireTime);
        request.put("accountCount", 100);
        request.put("status", status);
        request.put("remark", "identity lifecycle");
        return request;
    }

    private String issueTicket(Session session, String transport) {
        return success(request(session, HttpMethod.GET,
                "/api/system/realtime/ticket?transport=" + transport, null))
                .path("data").path("ticket").asText();
    }

    private long userVersion(long id) {
        return jdbc.queryForObject("SELECT auth_version FROM sys_user WHERE id=?", Long.class, id);
    }

    private long tenantVersion(long id) {
        return jdbc.queryForObject("SELECT auth_version FROM sys_tenant WHERE id=?", Long.class, id);
    }

    private HttpResponse<InputStream> openTicketSse(String ticket) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                        + RealtimeEndpoints.SSE_PATH + "?ticket=" + ticket))
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(10))
                .GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private SseConnection connectSse(String ticket) throws Exception {
        HttpResponse<InputStream> response = openTicketSse(ticket);
        assertThat(response.statusCode()).isEqualTo(200);
        return new SseConnection(response.body());
    }

    private SocketConnection connectWebSocket(String ticket) throws Exception {
        SocketListener listener = new SocketListener();
        URI uri = URI.create("ws://127.0.0.1:" + port + RealtimeEndpoints.WEB_SOCKET_PATH + "?ticket=" + ticket);
        WebSocket socket = HTTP.newWebSocketBuilder().buildAsync(uri, listener).get(10, TimeUnit.SECONDS);
        return new SocketConnection(socket, listener);
    }

    private final class Session {
        private final String token;

        private Session(String token) { this.token = token; }
        private String token() { return token; }
        private ResponseEntity<JsonNode> get(String path) { return request(this, HttpMethod.GET, path, null); }
        private ResponseEntity<JsonNode> put(String path, Object body) { return request(this, HttpMethod.PUT, path, body); }
        private ResponseEntity<JsonNode> delete(String path) { return request(this, HttpMethod.DELETE, path, null); }
        private ResponseEntity<JsonNode> post(String path, Object body) { return request(this, HttpMethod.POST, path, body); }
    }

    private static final class SseConnection implements AutoCloseable {
        private final BufferedReader reader;
        private final InputStream stream;

        private SseConnection(InputStream stream) {
            this.stream = stream;
            reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }

        private void awaitEvent(String content) throws Exception {
            String event = readEvent().get(8, TimeUnit.SECONDS);
            assertThat(event).contains(content);
        }

        private void awaitClosed() throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
            String event;
            do {
                long remaining = deadline - System.nanoTime();
                assertThat(remaining).isPositive();
                event = readEvent().get(remaining, TimeUnit.NANOSECONDS);
            } while (event != null);
        }

        private CompletableFuture<String> readEvent() {
            return CompletableFuture.supplyAsync(() -> {
                StringBuilder event = new StringBuilder();
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) {
                            if (event.length() > 0) return event.toString();
                            continue;
                        }
                        event.append(line).append('\n');
                    }
                    return event.length() == 0 ? null : event.toString();
                } catch (IOException error) {
                    return null;
                }
            });
        }

        @Override
        public void close() throws IOException {
            stream.close();
            reader.close();
        }
    }

    private static final class SocketListener implements WebSocket.Listener {
        private final CountDownLatch opened = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private final StringBuilder messages = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            opened.countDown();
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            synchronized (messages) { messages.append(data); }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closed.countDown();
            return CompletableFuture.completedFuture(null);
        }

        private String messages() {
            synchronized (messages) { return messages.toString(); }
        }
    }

    private static final class SocketConnection implements AutoCloseable {
        private final WebSocket socket;
        private final SocketListener listener;

        private SocketConnection(WebSocket socket, SocketListener listener) throws InterruptedException {
            this.socket = socket;
            this.listener = listener;
            assertThat(listener.opened.await(5, TimeUnit.SECONDS)).isTrue();
        }

        private void awaitMessage(String content) {
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(8)).untilAsserted(() ->
                    assertThat(listener.messages()).contains(content));
        }

        private void awaitClosed() throws InterruptedException {
            assertThat(listener.closed.await(8, TimeUnit.SECONDS)).isTrue();
        }

        private boolean isOpen() { return !socket.isOutputClosed() && !socket.isInputClosed(); }

        @Override
        public void close() { socket.abort(); }
    }
}
