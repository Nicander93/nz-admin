package com.nz.admin;

import cn.hutool.crypto.digest.BCrypt;
import com.nz.admin.framework.test.core.http.ApiTestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 使用同一个真实会话验证角色和菜单状态变化立即影响服务端鉴权。 */
@EnabledIfEnvironmentVariable(named = "NZ_TEST_PG_URL", matches = ".+")
@SpringBootTest(classes = NzAdminApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("pg-test")
@org.junit.jupiter.api.TestInstance(org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
class PermissionRevocationPostgresTest {
    private static final String CACHE_KEY_PREFIX = "permission-revocation-test-" + UUID.randomUUID();

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;

    private ApiTestClient session;

    @DynamicPropertySource
    static void isolateRedisKeys(DynamicPropertyRegistry registry) {
        registry.add("nz.cache.key-prefix", () -> CACHE_KEY_PREFIX);
    }

    @org.junit.jupiter.api.BeforeAll
    void seed() {
        cleanup();
        jdbc.update("INSERT INTO sys_user(id,tenant_id,dept_id,username,password,nickname,status) VALUES(970101,1,1,'revocation-test',?,'撤权测试',0)", BCrypt.hashpw("revocation-password"));
        jdbc.update("INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope) VALUES(970101,1,'可撤销角色','revocable-test',0,0,1),(970102,1,'其他角色','retained-test',0,0,1)");
        jdbc.update("INSERT INTO sys_menu(id,parent_id,name,path,component,type,perm,sort,visible,status) VALUES(970101,0,'受控配置','revoke-config','system/config/index','C','system:config:list',0,0,0),(970102,0,'伪目录权限','revoke-directory',null,'M','system:user:remove',0,0,0),(970103,0,'隐藏配置查询','revoke-hidden',null,'F','system:config:query',0,1,0),(970104,0,'禁用配置新增','revoke-disabled',null,'F','system:config:add',0,0,1),(970105,0,'其他角色权限','revoke-other',null,'F','system:role:list',0,0,0)");
        jdbc.update("INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES(1,970101,970101),(1,970101,970102)");
        jdbc.update("INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) VALUES(1,970101,970101),(1,970101,970102),(1,970101,970103),(1,970101,970104),(1,970102,970105)");
        session = new ApiTestClient(rest).login("default", "revocation-test", "revocation-password");
    }

    @BeforeEach
    void restorePermissions() {
        jdbc.update("UPDATE sys_role SET status=0 WHERE id IN (970101,970102)");
        jdbc.update("UPDATE sys_menu SET status=0 WHERE id=970101");
    }

    @org.junit.jupiter.api.AfterAll
    void cleanup() {
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id IN (970101,970102)");
        jdbc.update("DELETE FROM sys_user_role WHERE user_id=970101");
        jdbc.update("DELETE FROM sys_user WHERE id=970101");
        jdbc.update("DELETE FROM sys_role WHERE id IN (970101,970102)");
        jdbc.update("DELETE FROM sys_menu WHERE id BETWEEN 970101 AND 970105");
    }

    @Test
    void disablingRoleRevokesExistingSessionAndReenablingRestoresIt() {
        var api = login();
        api.ok(HttpMethod.GET, "/api/system/config/page", null);
        jdbc.update("UPDATE sys_role SET status=1 WHERE id=970101");
        assertDenied(api, "/api/system/config/page");
        var info = api.ok(HttpMethod.GET, "/api/auth/info", null);
        assertThat(info.path("roles").toString()).doesNotContain("revocable-test").contains("retained-test");
        assertThat(info.path("permissions").toString()).doesNotContain("system:config:list").contains("system:role:list");
        api.ok(HttpMethod.GET, "/api/system/role/page", null);
        assertThat(api.ok(HttpMethod.GET, "/api/auth/menus", null).toString()).doesNotContain("revoke-config");
        jdbc.update("UPDATE sys_role SET status=0 WHERE id=970101");
        api.ok(HttpMethod.GET, "/api/system/config/page", null);
    }

    @Test
    void disablingMenuRevokesItsPermissionWithoutRemovingOtherGrants() {
        var api = login();
        api.ok(HttpMethod.GET, "/api/system/config/page", null);
        jdbc.update("UPDATE sys_menu SET status=1 WHERE id=970101");
        assertDenied(api, "/api/system/config/page");
        var info = api.ok(HttpMethod.GET, "/api/auth/info", null);
        assertThat(info.path("permissions").toString()).doesNotContain("system:config:list").contains("system:config:query");
        assertThat(api.ok(HttpMethod.GET, "/api/auth/menus", null).toString()).doesNotContain("revoke-config");
        jdbc.update("UPDATE sys_menu SET status=0 WHERE id=970101");
        api.ok(HttpMethod.GET, "/api/system/config/page", null);
    }

    @Test
    void hiddenEnabledPermissionsWorkButDirectoriesAndDisabledButtonsDoNotAuthorize() {
        var api = login();
        var permissions = api.ok(HttpMethod.GET, "/api/auth/info", null).path("permissions").toString();
        assertThat(permissions).contains("system:config:query").doesNotContain("system:config:add", "system:user:remove");
        assertThat(api.ok(HttpMethod.GET, "/api/auth/menus", null).toString()).doesNotContain("revoke-hidden");
        assertThat(api.exchange(HttpMethod.DELETE, "/api/system/user/970101", null).path("code").asInt()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id=970101", Integer.class)).isEqualTo(1);
    }

    private ApiTestClient login() {
        return session;
    }

    private void assertDenied(ApiTestClient api, String path) {
        assertThat(api.exchange(HttpMethod.GET, path, null).path("code").asInt()).isEqualTo(403);
    }
}
