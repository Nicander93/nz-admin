package com.nz.admin;

import static org.assertj.core.api.Assertions.*;

import cn.dev33.satoken.SaManager;
import cn.hutool.crypto.digest.BCrypt;

import com.fasterxml.jackson.databind.JsonNode;
import com.nz.admin.framework.auth.core.RedisSaTokenDao;
import com.nz.admin.framework.test.core.http.ApiTestClient;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

/** 真实引擎、HTTP 身份和 PostgreSQL 迁移；显式集群模式验证 Redis DAO 装配。 */
@EnabledIfEnvironmentVariable(named = "NZ_TEST_PG_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "NZ_TEST_REDIS_PORT", matches = "[0-9]+")
@SpringBootTest(
        classes = NzAdminApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "warm-flow.enabled=true",
            "nz.cluster.enabled=true",
            "spring.data.redis.port=${NZ_TEST_REDIS_PORT}",
            "nz.cache.key-prefix=warm-integration"
        })
@ActiveProfiles("pg-test")
class WarmFlowPostgresTest {
    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.nz.admin.framework.cache.core.AtomicStateStore states;
    private final String code = "test_" + UUID.randomUUID().toString().replace("-", "");
    private Long definitionId;
    private Long instanceId;
    private Long foreignDefinitionId;

    @BeforeEach
    void seed() {
        // 清理本测试命名空间中的登录窗口，各用例仍经过真实限流切面。
        states.take("rate-limit:POST:/api/auth/login:127.0.0.1");
        states.take("rate-limit:POST:/api/auth/login:0:0:0:0:0:0:0:1");
        jdbc.update(
                "UPDATE sys_user SET password=? WHERE tenant_id=1 AND username='admin'",
                BCrypt.hashpw("admin123"));
        jdbc.update(
                "INSERT INTO sys_user(id,tenant_id,dept_id,username,password,nickname,status)"
                        + " VALUES(910001,1,1,'engine-outsider',?,'外部用户',0)",
                BCrypt.hashpw("test-password"));
        jdbc.update(
                "INSERT INTO sys_user_role(tenant_id,user_id,role_id) SELECT 1,910001,id FROM"
                        + " sys_role WHERE tenant_id=1 AND role_key='admin'");
        jdbc.update(
                "INSERT INTO sys_tenant(id,tenant_code,tenant_name,package_id,account_count,status)"
                        + " SELECT 910002,'engine-tenant','引擎隔离测试',package_id,100,0 FROM sys_tenant"
                        + " WHERE id=1");
        jdbc.update(
                "INSERT INTO sys_dept(id,tenant_id,parent_id,name,sort,status)"
                        + " VALUES(910002,910002,0,'引擎部门',0,0)");
        jdbc.update(
                "INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope)"
                        + " VALUES(910002,910002,'引擎管理员','admin',0,0,1)");
        jdbc.update(
                "INSERT INTO sys_user(id,tenant_id,dept_id,username,password,nickname,status)"
                        + " VALUES(910002,910002,910002,'engine-foreign',?,'其他租户',0)",
                BCrypt.hashpw("test-password"));
        jdbc.update(
                "INSERT INTO sys_user_role(tenant_id,user_id,role_id)"
                        + " VALUES(910002,910002,910002)");
        jdbc.update(
                "INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) SELECT 910002,910002,id FROM"
                        + " sys_menu WHERE perm LIKE 'workflow:engine:%'");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM nz_idempotency WHERE scope LIKE '%/api/workflow/engine/%'");
        if (instanceId != null) {
            jdbc.update(
                    "DELETE FROM flow_user WHERE associated IN (SELECT id FROM flow_task WHERE"
                            + " instance_id=?)",
                    instanceId);
            jdbc.update(
                    "DELETE FROM flow_user WHERE associated IN (SELECT id FROM flow_his_task WHERE"
                            + " instance_id=?)",
                    instanceId);
            jdbc.update("DELETE FROM flow_task WHERE instance_id=?", instanceId);
            jdbc.update("DELETE FROM flow_his_task WHERE instance_id=?", instanceId);
            jdbc.update("DELETE FROM flow_instance WHERE id=?", instanceId);
        }
        for (Long id : Arrays.asList(definitionId, foreignDefinitionId))
            if (id != null) {
                jdbc.update("DELETE FROM flow_skip WHERE definition_id=?", id);
                jdbc.update("DELETE FROM flow_node WHERE definition_id=?", id);
                jdbc.update("DELETE FROM flow_definition WHERE id=?", id);
            }
        jdbc.update("DELETE FROM sys_user_role WHERE user_id=910001");
        jdbc.update("DELETE FROM sys_user WHERE id=910001");
        jdbc.update("DELETE FROM sys_user_role WHERE user_id=910002");
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id=910002");
        jdbc.update("DELETE FROM sys_user WHERE id=910002");
        jdbc.update("DELETE FROM sys_role WHERE id=910002");
        jdbc.update("DELETE FROM sys_dept WHERE id=910002");
        jdbc.update("DELETE FROM sys_tenant WHERE id=910002");
    }

    @Test
    void newEngineCompletesWithoutChangingLegacyInstancesAndDeniesUnrelatedUsers() {
        assertThat(SaManager.getSaTokenDao()).isInstanceOf(RedisSaTokenDao.class);
        long legacy =
                jdbc.queryForObject("SELECT count(*) FROM nz_flow_instance_legacy", Long.class);
        var api = new ApiTestClient(rest).login("default", "admin", "admin123");
        var outsider = new ApiTestClient(rest).login("default", "engine-outsider", "test-password");
        long adminId =
                jdbc.queryForObject(
                        "SELECT id FROM sys_user WHERE tenant_id=1 AND username='admin'",
                        Long.class);
        var nodes =
                List.of(
                        Map.of(
                                "nodeCode",
                                "start",
                                "nodeName",
                                "开始",
                                "nodeType",
                                0,
                                "skipList",
                                List.of(Map.of("nextNodeCode", "review", "skipType", "PASS"))),
                        Map.of(
                                "nodeCode",
                                "review",
                                "nodeName",
                                "审核",
                                "nodeType",
                                1,
                                "permissionFlag",
                                "user:" + adminId,
                                "skipList",
                                List.of(Map.of("nextNodeCode", "end", "skipType", "PASS"))),
                        Map.of("nodeCode", "end", "nodeName", "结束", "nodeType", 2));
        definitionId =
                api.ok(
                                HttpMethod.POST,
                                "/api/workflow/engine/definitions",
                                Map.of("flowCode", code, "flowName", "真实测试", "nodeList", nodes))
                        .asLong();
        api.ok(
                HttpMethod.POST,
                "/api/workflow/engine/definitions/" + definitionId + "/publish",
                null);
        var request =
                Map.of(
                        "flowCode",
                        code,
                        "businessId",
                        "business-" + code,
                        "variables",
                        Map.of("amount", 10));
        String key = UUID.randomUUID().toString();
        instanceId = okWithKey(api, "/api/workflow/engine/instances", request, key).asLong();
        assertThat(okWithKey(api, "/api/workflow/engine/instances", request, key).asLong())
                .isEqualTo(instanceId);
        var snapshot = api.ok(HttpMethod.GET, "/api/workflow/engine/instances/" + instanceId, null);
        long task = snapshot.path("tasks").get(0).path("id").asLong();
        assertThat(
                        outsider.exchange(
                                        HttpMethod.GET,
                                        "/api/workflow/engine/instances/" + instanceId,
                                        null)
                                .path("code")
                                .asInt())
                .isNotEqualTo(200);
        assertThat(
                        outsider.exchange(
                                        HttpMethod.POST,
                                        "/api/workflow/engine/tasks/" + task + "/action",
                                        Map.of("type", "PASS"),
                                        Map.of("Idempotency-Key", UUID.randomUUID().toString()))
                                .path("code")
                                .asInt())
                .isNotEqualTo(200);
        okWithKey(
                api,
                "/api/workflow/engine/tasks/" + task + "/action",
                Map.of("type", "PASS", "comment", "同意"),
                UUID.randomUUID().toString());
        assertThat(
                        api.ok(HttpMethod.GET, "/api/workflow/engine/instances/" + instanceId, null)
                                .path("tasks"))
                .isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM nz_flow_instance_legacy", Long.class))
                .isEqualTo(legacy);
    }

    @Test
    void parallelBranchesRemainPendingUntilBothAreApproved() {
        var api = new ApiTestClient(rest).login("default", "admin", "admin123");
        long adminId =
                jdbc.queryForObject(
                        "SELECT id FROM sys_user WHERE tenant_id=1 AND username='admin'",
                        Long.class);
        var nodes =
                List.of(
                        node("start", 0, null, "fork"), node("fork", 4, null, "a", "b"),
                        node("a", 1, "user:" + adminId, "join"),
                                node("b", 1, "user:" + adminId, "join"),
                        node("join", 4, null, "end"), node("end", 2, null));
        definitionId =
                api.ok(
                                HttpMethod.POST,
                                "/api/workflow/engine/definitions",
                                Map.of("flowCode", code, "flowName", "并行测试", "nodeList", nodes))
                        .asLong();
        api.ok(
                HttpMethod.POST,
                "/api/workflow/engine/definitions/" + definitionId + "/publish",
                null);
        instanceId =
                okWithKey(
                                api,
                                "/api/workflow/engine/instances",
                                Map.of("flowCode", code, "businessId", "parallel-" + code),
                                UUID.randomUUID().toString())
                        .asLong();
        var tasks =
                api.ok(HttpMethod.GET, "/api/workflow/engine/instances/" + instanceId, null)
                        .path("tasks");
        assertThat(tasks.size()).isEqualTo(2);
        for (var task : tasks)
            okWithKey(
                    api,
                    "/api/workflow/engine/tasks/" + task.path("id").asText() + "/action",
                    Map.of("type", "PASS"),
                    UUID.randomUUID().toString());
        assertThat(
                        api.ok(HttpMethod.GET, "/api/workflow/engine/instances/" + instanceId, null)
                                .path("tasks")
                                .size())
                .isZero();
    }

    @Test
    void sameFlowCodeAndPublishingAreIsolatedBetweenTenants() {
        var api = new ApiTestClient(rest).login("default", "admin", "admin123");
        var foreign =
                new ApiTestClient(rest).login("engine-tenant", "engine-foreign", "test-password");
        var request =
                Map.of(
                        "flowCode",
                        code,
                        "flowName",
                        "隔离发布",
                        "nodeList",
                        List.of(node("start", 0, null, "end"), node("end", 2, null)));
        definitionId =
                api.ok(HttpMethod.POST, "/api/workflow/engine/definitions", request).asLong();
        api.ok(
                HttpMethod.POST,
                "/api/workflow/engine/definitions/" + definitionId + "/publish",
                null);
        foreignDefinitionId =
                foreign.ok(HttpMethod.POST, "/api/workflow/engine/definitions", request).asLong();
        foreign.ok(
                HttpMethod.POST,
                "/api/workflow/engine/definitions/" + foreignDefinitionId + "/publish",
                null);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT is_publish FROM flow_definition WHERE id=?",
                                Integer.class,
                                definitionId))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT tenant_id FROM flow_definition WHERE id=?",
                                String.class,
                                foreignDefinitionId))
                .isEqualTo("910002");
        assertThat(
                        foreign.exchange(
                                        HttpMethod.GET,
                                        "/api/workflow/engine/definitions/" + definitionId,
                                        null)
                                .path("code")
                                .asInt())
                .isNotEqualTo(200);
        assertThat(
                        api.exchange(
                                        HttpMethod.POST,
                                        "/api/workflow/engine/definitions/"
                                                + foreignDefinitionId
                                                + "/publish",
                                        null)
                                .path("code")
                                .asInt())
                .isNotEqualTo(200);
    }

    @Test
    void countersignRequiresEveryAssignedApprover() {
        var api = new ApiTestClient(rest).login("default", "admin", "admin123");
        var second = new ApiTestClient(rest).login("default", "engine-outsider", "test-password");
        long adminId =
                jdbc.queryForObject(
                        "SELECT id FROM sys_user WHERE tenant_id=1 AND username='admin'",
                        Long.class);
        var review = node("review", 1, "user:" + adminId + ",user:910001", "end");
        review.put("nodeRatio", "100");
        var unsafe = node("review", 1, "user:" + adminId + ",#{@service.execute()}", "end");
        assertThat(
                        api.exchange(
                                        HttpMethod.POST,
                                        "/api/workflow/engine/definitions",
                                        Map.of(
                                                "flowCode",
                                                code,
                                                "flowName",
                                                "禁止执行表达式",
                                                "nodeList",
                                                List.of(
                                                        node("start", 0, null, "review"),
                                                        unsafe,
                                                        node("end", 2, null))))
                                .path("code")
                                .asInt())
                .isNotEqualTo(200);
        var request =
                Map.of(
                        "flowCode",
                        code,
                        "flowName",
                        "会签测试",
                        "nodeList",
                        List.of(node("start", 0, null, "review"), review, node("end", 2, null)));
        definitionId =
                api.ok(HttpMethod.POST, "/api/workflow/engine/definitions", request).asLong();
        api.ok(
                HttpMethod.POST,
                "/api/workflow/engine/definitions/" + definitionId + "/publish",
                null);
        instanceId =
                okWithKey(
                                api,
                                "/api/workflow/engine/instances",
                                Map.of("flowCode", code, "businessId", "sign-" + code),
                                UUID.randomUUID().toString())
                        .asLong();
        String task =
                api.ok(HttpMethod.GET, "/api/workflow/engine/instances/" + instanceId, null)
                        .path("tasks")
                        .get(0)
                        .path("id")
                        .asText();
        okWithKey(
                api,
                "/api/workflow/engine/tasks/" + task + "/action",
                Map.of("type", "PASS"),
                UUID.randomUUID().toString());
        assertThat(
                        api.ok(HttpMethod.GET, "/api/workflow/engine/instances/" + instanceId, null)
                                .path("tasks")
                                .size())
                .isEqualTo(1);
        okWithKey(
                second,
                "/api/workflow/engine/tasks/" + task + "/action",
                Map.of("type", "PASS"),
                UUID.randomUUID().toString());
        assertThat(
                        api.ok(HttpMethod.GET, "/api/workflow/engine/instances/" + instanceId, null)
                                .path("tasks")
                                .size())
                .isZero();
    }

    private Map<String, Object> node(String code, int type, String permission, String... targets) {
        var node = new HashMap<String, Object>();
        node.put("nodeCode", code);
        node.put("nodeName", code);
        node.put("nodeType", type);
        if (permission != null) node.put("permissionFlag", permission);
        node.put(
                "skipList",
                Arrays.stream(targets)
                        .map(target -> Map.of("nextNodeCode", target, "skipType", "PASS"))
                        .toList());
        return node;
    }

    private JsonNode okWithKey(ApiTestClient api, String path, Object body, String key) {
        var response = api.exchange(HttpMethod.POST, path, body, Map.of("Idempotency-Key", key));
        assertThat(response.path("code").asInt()).as(response.path("msg").asText()).isEqualTo(200);
        return response.path("data");
    }
}
