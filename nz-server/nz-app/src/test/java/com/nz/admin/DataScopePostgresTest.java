package com.nz.admin;

import static org.assertj.core.api.Assertions.*;

import cn.hutool.crypto.digest.BCrypt;

import com.nz.admin.framework.test.core.http.ApiTestClient;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

/** 只在显式提供独立测试库时执行：使用真实 Flyway 迁移及 HTTP 接口。 */
@EnabledIfEnvironmentVariable(named = "NZ_TEST_PG_URL", matches = ".+")
@SpringBootTest(
        classes = NzAdminApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("pg-test")
class DataScopePostgresTest {
    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        cleanup();
        jdbc.update(
                "INSERT INTO sys_dept(id,tenant_id,parent_id,name,sort,status)"
                    + " VALUES(900001,1,1,'测试父部门',0,0),(900002,1,900001,'测试子部门',0,0),(900003,1,1,'测试其他部门',0,0)");
        jdbc.update(
                "INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope)"
                    + " VALUES(900001,1,'测试角色','scope-integration',0,0,3)");
        jdbc.update("INSERT INTO sys_tenant(id,tenant_code,tenant_name,package_id,account_count,status) SELECT 900004,'scope-tenant','隔离测试',package_id,100,0 FROM sys_tenant WHERE id=1");
        jdbc.update("INSERT INTO sys_dept(id,tenant_id,parent_id,name,sort,status) VALUES(900004,900004,0,'其他租户',0,0)");
        jdbc.update("INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope) VALUES(900004,900004,'隔离管理员','admin',0,0,1),(900005,1,'无关报表角色','unrelated-report',0,0,1)");
        String password = BCrypt.hashpw("test-password");
        jdbc.update(
                "INSERT INTO sys_user(id,tenant_id,dept_id,username,password,nickname,status)"
                    + " VALUES(900001,1,900001,'scope-owner',?,'本人',0),(900002,1,900002,'scope-child',?,'下级',0),(900003,1,900003,'scope-other',?,'其他',0)",
                password,
                password,
                password);
        jdbc.update("INSERT INTO sys_user(id,tenant_id,dept_id,username,password,nickname,status) VALUES(900004,900004,900004,'scope-isolated',?,'隔离用户',0)", password);
        jdbc.update("INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES(1,900001,900001),(1,900001,900005),(900004,900004,900004)");
        jdbc.update("INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) SELECT 1,900005,id FROM sys_menu WHERE perm='system:role:list'");
        jdbc.update("INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) SELECT 900004,900004,id FROM sys_menu WHERE perm IN ('system:user:list','system:user:query','system:user:edit','system:user:remove')");
        jdbc.update(
                "INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) SELECT 1,900001,id FROM"
                    + " sys_menu WHERE perm IN"
                    + " ('system:user:list','system:user:query','system:user:edit','system:user:remove')");
        jdbc.update(
                "UPDATE sys_user SET password=? WHERE tenant_id=1 AND username='admin'",
                BCrypt.hashpw("admin123"));
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM sys_role_dept WHERE role_id IN (900001,900004,900005)");
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id IN (900001,900004,900005)");
        jdbc.update("DELETE FROM sys_user_role WHERE user_id IN (900001,900002,900003,900004)");
        jdbc.update("DELETE FROM sys_user WHERE id IN (900001,900002,900003,900004)");
        jdbc.update("DELETE FROM sys_role WHERE id IN (900001,900004,900005)");
        jdbc.update("DELETE FROM sys_dept WHERE id IN (900001,900002,900003,900004)");
        jdbc.update("DELETE FROM sys_tenant WHERE id=900004");
    }

    @Test
    void roleConfigurationControlsRealHttpReadsAndWrites() {
        var api = new ApiTestClient(rest).login("default", "scope-owner", "test-password");
        String page = "/api/system/user/page?pageNum=1&pageSize=10&username=scope-";
        var isolated = new ApiTestClient(rest).login("scope-tenant", "scope-isolated", "test-password");
        assertThat(isolated.ok(HttpMethod.GET, page, null).path("records").get(0).path("id").asLong()).isEqualTo(900004);
        assertThat(isolated.exchange(HttpMethod.GET, "/api/system/user/900001", null).path("code").asInt()).isNotEqualTo(200);
        assertThat(api.ok(HttpMethod.GET, page, null).path("total").asInt()).isEqualTo(1);
        assertThat(api.exchange(HttpMethod.GET, "/api/system/config/page", null).path("code").asInt()).isNotEqualTo(200);
        assertThat(
                        api.exchange(HttpMethod.GET, "/api/system/user/900003", null)
                                .path("code")
                                .asInt())
                .isNotEqualTo(200);
        assertThat(
                        api.exchange(
                                        HttpMethod.PUT,
                                        "/api/system/user",
                                        Map.of("id", 900003, "nickname", "越权修改"))
                                .path("code")
                                .asInt())
                .isNotEqualTo(200);
        assertThat(
                        api.exchange(HttpMethod.DELETE, "/api/system/user/900003", null)
                                .path("code")
                                .asInt())
                .isNotEqualTo(200);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT nickname FROM sys_user WHERE id=900003", String.class))
                .isEqualTo("其他");

        jdbc.update("UPDATE sys_role SET data_scope=4 WHERE id=900001");
        assertThat(api.ok(HttpMethod.GET, page, null).path("total").asInt()).isEqualTo(2);
        var admin = new ApiTestClient(rest).login("default", "admin", "admin123");
        admin.ok(
                HttpMethod.PUT,
                "/api/system/role",
                Map.of(
                        "id",
                        900001,
                        "name",
                        "测试角色",
                        "roleKey",
                        "scope-integration",
                        "status",
                        0,
                        "dataScope",
                        2,
                        "deptIds",
                        List.of(900003)));
        assertThat(
                        admin.ok(HttpMethod.GET, "/api/system/role/900001", null)
                                .path("deptIds")
                                .get(0)
                                .asLong())
                .isEqualTo(900003);
        assertThat(api.ok(HttpMethod.GET, page, null).path("records").get(0).path("id").asLong())
                .isEqualTo(900003);
        assertThat(api.ok(HttpMethod.GET, "/api/system/profile", null).path("id").asLong())
                .isEqualTo(900001);
        api.ok(HttpMethod.PUT, "/api/system/profile",
                Map.of("nickname", "个人中心修改", "gender", "0", "email", "", "phone", ""));
        assertThat(jdbc.queryForObject("SELECT nickname FROM sys_user WHERE id=900001", String.class))
                .isEqualTo("个人中心修改");

    }
}
