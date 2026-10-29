package com.nz.admin;

import cn.hutool.crypto.digest.BCrypt;
import com.fasterxml.jackson.databind.JsonNode;
import com.nz.admin.framework.realtime.core.*;
import com.nz.admin.support.RealtimeProbe;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** 最小权限普通用户的通知scope、实时撤权和真实两种传输。 */
@EnabledIfEnvironmentVariable(named="NZ_TEST_PG_URL", matches=".+")
@EnabledIfEnvironmentVariable(named="NZ_TEST_REDIS_PORT", matches="[0-9]+")
@SpringBootTest(classes=NzAdminApplication.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"nz.cluster.enabled=true", "nz.redis.enabled=true", "spring.data.redis.port=${NZ_TEST_REDIS_PORT}"})
@ActiveProfiles("pg-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RealtimeAuthorizationPostgresTest {
    private static final long TENANT=980600L, NOTIFY=980610L, GENERAL=980611L, DENIED=980612L;
    private static final long MSG_ROLE=980620L, DIAG_ROLE=980621L, PACKAGE=980630L;
    private static final long MSG_MENU=980640L, VIEW_MENU=980641L, SEND_MENU=980642L;
    private static final String PREFIX="realtime-authorization-"+UUID.randomUUID();
    private final String suffix=UUID.randomUUID().toString().replace("-", "").substring(0,12);
    private final String tenantCode="rt-auth-"+suffix;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestRestTemplate rest;
    @Autowired RealtimePublisher publisher;
    @Autowired RealtimeConnectionManager connections;
    @Autowired TransactionTemplate transactions;
    @LocalServerPort int port;
    private String notifyToken, generalToken, deniedToken;

    @DynamicPropertySource
    static void namespace(DynamicPropertyRegistry properties) { properties.add("nz.cache.key-prefix", () -> PREFIX); }

    @BeforeAll
    void fixtures() throws InterruptedException {
        cleanup();
        jdbc.update("INSERT INTO sys_menu(id,parent_id,name,type,perm,status,visible,sort) VALUES(?,0,'rt-msg','F','system:message:list',0,0,0),"
                + "(?,0,'rt-view','F','system:realtime:view',0,0,0),(?,0,'rt-send','F','system:realtime:send',0,0,0)", MSG_MENU,VIEW_MENU,SEND_MENU);
        jdbc.update("INSERT INTO sys_tenant_package(id,package_name,status) VALUES(?,?,0)",PACKAGE,"rt-auth-"+suffix);
        jdbc.update("INSERT INTO sys_tenant_package_menu(package_id,menu_id) VALUES(?,?),(?,?),(?,?)", PACKAGE,MSG_MENU,PACKAGE,VIEW_MENU,PACKAGE,SEND_MENU);
        jdbc.update("INSERT INTO sys_tenant(id,tenant_code,tenant_name,package_id,account_count,status) VALUES(?,?,?, ?,100,0)",TENANT,tenantCode,"Realtime Authorization",PACKAGE);
        jdbc.update("INSERT INTO sys_role(id,tenant_id,name,role_key,status,sort,data_scope) VALUES(?,?,?, ?,0,0,1),(?,?,?, ?,0,0,1)",
                MSG_ROLE,TENANT,"Message only","rt-msg-"+suffix,DIAG_ROLE,TENANT,"Diagnostic only","rt-diag-"+suffix);
        for (long user : new long[]{NOTIFY,GENERAL,DENIED}) jdbc.update("INSERT INTO sys_user(id,tenant_id,username,password,nickname,status) VALUES(?,?,?,?,?,0)",
                user,TENANT,username(user),BCrypt.hashpw("realtime-authorization-password"),"Realtime Fixture");
        restoreGrants();
        notifyToken=login(NOTIFY);Thread.sleep(1050);
        generalToken=login(GENERAL);Thread.sleep(1050);
        deniedToken=login(DENIED);
    }

    @BeforeEach
    void resetPermissions() { restoreGrants(); }

    @AfterEach
    void closeConnections() {
        for (long user : new long[]{NOTIFY,GENERAL,DENIED}) connections.disconnectUser(TENANT,user);
        restoreGrants();
    }

    @AfterAll
    void removeFixtures() { cleanup(); }

    @Test
    void notificationsAcceptOnlyPersonalMessagesAndNeverRequireDiagnosticPermission() throws Exception {
        rejected(get(notifyToken,"/api/system/realtime/ticket?transport=SSE"));
        rejected(get(deniedToken,"/api/system/realtime/notifications/ticket"));
        rejected(get(generalToken,"/api/system/realtime/notifications/ticket"));
        try (var sse=RealtimeProbe.sse(base(),ticket(notifyToken,"SSE",true));
             var socket=RealtimeProbe.socket(base(),ticket(notifyToken,"WEBSOCKET",true));
             var general=RealtimeProbe.sse(base(),ticket(generalToken,"SSE",false))) {
            publishTenant("system-message","denied-tenant");general.await("denied-tenant");
            publisher.broadcast(RealtimeMessage.of("system-message-state",Map.of("marker","denied-broadcast")));general.await("denied-broadcast");
            publishUser(NOTIFY,"test","denied-diagnostic");
            publishUser(GENERAL,"system-message","other-user");general.await("other-user");
            publisher.publishToUser(TENANT+1,NOTIFY,RealtimeMessage.of("system-message",Map.of("marker","other-tenant")));
            publishUser(NOTIFY,"system-message","allowed-notification");sse.await("allowed-notification");socket.await("allowed-notification");
            publishUser(NOTIFY,"system-message-state","allowed-state");sse.await("allowed-state");socket.await("allowed-state");
            for (RealtimeProbe.Events events : new RealtimeProbe.Events[]{sse,socket}) {
                assertThat(events.events().toString()).doesNotContain("denied-tenant","denied-broadcast","denied-diagnostic","other-user","other-tenant");
                assertThat(events.isClosed()).isFalse();
            }
        }
    }

    @Test
    void roleMenuRevocationClosesIdleConnectionsAndInvalidatesPendingTickets() throws Exception {
        String pendingSse=ticket(notifyToken,"SSE",true),pendingSocket=ticket(notifyToken,"WEBSOCKET",true);
        try (var sse=RealtimeProbe.sse(base(),ticket(notifyToken,"SSE",true));
             var socket=RealtimeProbe.socket(base(),ticket(notifyToken,"WEBSOCKET",true))) {
            long version=userVersion(NOTIFY);
            jdbc.update("DELETE FROM sys_role_menu WHERE role_id=? AND menu_id=?",MSG_ROLE,MSG_MENU);
            sse.awaitClosed();socket.awaitClosed();
            assertThat(userVersion(NOTIFY)).isEqualTo(version);
            rejected(get(notifyToken,"/api/system/realtime/notifications/ticket"));
            assertThat(RealtimeProbe.sseStatus(base(),pendingSse)).isEqualTo(401);
            RealtimeProbe.assertSocketRejected(base(),pendingSocket);
            restoreGrants();
            assertThat(RealtimeProbe.sseStatus(base(),pendingSse)).isEqualTo(401);
            RealtimeProbe.assertSocketRejected(base(),pendingSocket);
            try (var fresh=RealtimeProbe.sse(base(),ticket(notifyToken,"SSE",true))) {
                publishUser(NOTIFY,"system-message","fresh-after-regrant");fresh.await("fresh-after-regrant");
                assertThat(sse.isClosed()).isTrue();assertThat(socket.isClosed()).isTrue();
            }
        }
    }

    @Test
    void disablingRoleOrMenuAndReassigningUserRolesRevokesGeneralConnections() throws Exception {
        try (var sse=RealtimeProbe.sse(base(),ticket(generalToken,"SSE",false));
             var socket=RealtimeProbe.socket(base(),ticket(generalToken,"WEBSOCKET",false))) {
            jdbc.update("UPDATE sys_role SET status=1 WHERE id=?",DIAG_ROLE);
            sse.awaitClosed();socket.awaitClosed();
            rejected(get(generalToken,"/api/system/realtime/ticket"));
        }
        restoreGrants();
        try (var sse=RealtimeProbe.sse(base(),ticket(generalToken,"SSE",false))) {
            jdbc.update("UPDATE sys_menu SET status=1 WHERE id=?",VIEW_MENU);
            sse.awaitClosed();rejected(get(generalToken,"/api/system/realtime/ticket"));
        }
        restoreGrants();
        try (var sse=RealtimeProbe.sse(base(),ticket(generalToken,"SSE",false));
             var socket=RealtimeProbe.socket(base(),ticket(generalToken,"WEBSOCKET",false));
             var notifications=RealtimeProbe.sse(base(),ticket(notifyToken,"SSE",true))) {
            jdbc.update("DELETE FROM sys_user_role WHERE tenant_id=? AND user_id=?",TENANT,GENERAL);
            sse.awaitClosed();socket.awaitClosed();
            publishUser(NOTIFY,"system-message","other-scope-survives");notifications.await("other-scope-survives");
            assertThat(userVersion(GENERAL)).isZero();
        }
    }

    @Test
    void uncommittedPermissionChangesAndRollbackDoNotRevokeOtherConnections() throws Exception {
        try (var sse=RealtimeProbe.sse(base(),ticket(notifyToken,"SSE",true));
             var socket=RealtimeProbe.socket(base(),ticket(notifyToken,"WEBSOCKET",true))) {
            transactions.executeWithoutResult(status -> {
                jdbc.update("DELETE FROM sys_role_menu WHERE role_id=? AND menu_id=?",MSG_ROLE,MSG_MENU);
                success(get(notifyToken,"/api/system/realtime/notifications/ticket"));
                publishUser(NOTIFY,"system-message","uncommitted-grant-removal");
                sse.await("uncommitted-grant-removal");socket.await("uncommitted-grant-removal");
                status.setRollbackOnly();
            });
            assertThat(jdbc.queryForObject("SELECT count(*) FROM sys_role_menu WHERE role_id=? AND menu_id=?",Integer.class,MSG_ROLE,MSG_MENU)).isEqualTo(1);
            publishUser(NOTIFY,"system-message-state","after-permission-rollback");sse.await("after-permission-rollback");socket.await("after-permission-rollback");
            assertThat(sse.isClosed()).isFalse();assertThat(socket.isClosed()).isFalse();
        }
    }

    private String base() { return "http://127.0.0.1:"+port; }
    private String username(long id) { return "rt-user-"+id+"-"+suffix; }
    private String login(long id) { return success(rest.postForEntity("/api/auth/login",Map.of("tenantCode",tenantCode,"clientId","nz-web-account","username",username(id),"password","realtime-authorization-password"),JsonNode.class)).path("data").asText(); }
    private ResponseEntity<JsonNode> get(String token,String path) {
        HttpHeaders headers=new HttpHeaders();headers.set("Authorization",token);
        return rest.exchange(path,HttpMethod.GET,new HttpEntity<>(headers),JsonNode.class);
    }
    private JsonNode success(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asInt()).as(response.getBody().toString()).isEqualTo(200);return response.getBody();
    }
    private void rejected(ResponseEntity<JsonNode> response) { assertThat(!response.getStatusCode().is2xxSuccessful()||response.getBody().path("code").asInt()!=200).isTrue(); }
    private String ticket(String token,String transport,boolean notifications) {
        return success(get(token,"/api/system/realtime/"+(notifications?"notifications/":"")+"ticket?transport="+transport)).path("data").path("ticket").asText();
    }
    private long userVersion(long user) { return jdbc.queryForObject("SELECT auth_version FROM sys_user WHERE id=?",Long.class,user); }
    private void publishUser(long user,String type,String marker) { publisher.publishToUser(TENANT,user,RealtimeMessage.of(type,Map.of("marker",marker))); }
    private void publishTenant(String type,String marker) { publisher.publishToTenant(TENANT,RealtimeMessage.of(type,Map.of("marker",marker))); }
    private void restoreGrants() {
        jdbc.update("UPDATE sys_role SET status=0 WHERE id IN (?,?)",MSG_ROLE,DIAG_ROLE);
        jdbc.update("UPDATE sys_menu SET status=0 WHERE id IN (?,?,?)",MSG_MENU,VIEW_MENU,SEND_MENU);
        jdbc.update("INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) VALUES(?,?,?),(?,?,?),(?,?,?) ON CONFLICT(role_id,menu_id) DO NOTHING",TENANT,MSG_ROLE,MSG_MENU,TENANT,DIAG_ROLE,VIEW_MENU,TENANT,DIAG_ROLE,SEND_MENU);
        jdbc.update("INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES(?,?,?),(?,?,?) ON CONFLICT(user_id,role_id) DO NOTHING",TENANT,NOTIFY,MSG_ROLE,TENANT,GENERAL,DIAG_ROLE);
    }
    private void cleanup() {
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id IN (?,?)",MSG_ROLE,DIAG_ROLE);
        jdbc.update("DELETE FROM sys_user_role WHERE user_id IN (?,?,?)",NOTIFY,GENERAL,DENIED);
        jdbc.update("DELETE FROM sys_user WHERE id IN (?,?,?)",NOTIFY,GENERAL,DENIED);
        jdbc.update("DELETE FROM sys_role WHERE id IN (?,?)",MSG_ROLE,DIAG_ROLE);
        jdbc.update("DELETE FROM sys_tenant WHERE id=?",TENANT);
        jdbc.update("DELETE FROM sys_tenant_package_menu WHERE package_id=?",PACKAGE);
        jdbc.update("DELETE FROM sys_tenant_package WHERE id=?",PACKAGE);
        jdbc.update("DELETE FROM sys_menu WHERE id IN (?,?,?)",MSG_MENU,VIEW_MENU,SEND_MENU);
    }
}
