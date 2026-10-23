package com.nz.admin;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.JsonNode;
import com.nz.admin.modules.demo.entity.vo.DemoItemExcelExportRow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Uses the explicitly configured isolated PostgreSQL instance and real HTTP security pipeline. */
@EnabledIfEnvironmentVariable(named = "NZ_TEST_PG_URL", matches = ".+")
@SpringBootTest(classes = NzAdminApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("pg-test")
class DemoItemExcelPostgresTest {
    private static final long USER_ID = 930001;
    private static final long ROLE_ID = 930001;
    private static final long LIMITED_USER_ID = 930002;
    private static final long LIMITED_ROLE_ID = 930002;
    private String token;

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        cleanup();
        jdbc.update("INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope) VALUES(?,1,'Excel 自身范围','excel-self',0,0,5)", ROLE_ID);
        jdbc.update("INSERT INTO sys_user(id,tenant_id,dept_id,username,password,nickname,status) VALUES(?,1,1,'excel-self','seed','Excel 用户',0)", USER_ID);
        jdbc.update("UPDATE sys_user SET password=? WHERE id=?", cn.hutool.crypto.digest.BCrypt.hashpw("test-password"), USER_ID);
        jdbc.update("INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES(1,?,?)", USER_ID, ROLE_ID);
        jdbc.update("INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) SELECT 1,?,id FROM sys_menu WHERE perm IN ('demo:item:list','demo:item:import','demo:item:export')", ROLE_ID);
        jdbc.update("INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope) VALUES(?,1,'仅查询','excel-query',0,0,5)", LIMITED_ROLE_ID);
        jdbc.update("INSERT INTO sys_user(id,tenant_id,dept_id,username,password,nickname,status) VALUES(?,1,1,'excel-query','seed','只读用户',0)", LIMITED_USER_ID);
        jdbc.update("UPDATE sys_user SET password=? WHERE id=?", cn.hutool.crypto.digest.BCrypt.hashpw("test-password"), LIMITED_USER_ID);
        jdbc.update("INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES(1,?,?)", LIMITED_USER_ID, LIMITED_ROLE_ID);
        jdbc.update("INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) SELECT 1,?,id FROM sys_menu WHERE perm='demo:item:list'", LIMITED_ROLE_ID);
        jdbc.update("UPDATE sys_user SET password=? WHERE tenant_id=1 AND username='admin'", cn.hutool.crypto.digest.BCrypt.hashpw("admin123"));
        JsonNode login = rest.postForObject("/api/auth/login", Map.of("tenantCode", "default", "clientId", "nz-web-account", "username", "excel-self", "password", "test-password"), JsonNode.class);
        token = login.path("data").asText();
        jdbc.update("INSERT INTO demo_item(tenant_id,owner_id,name,category,status,sort,remark) VALUES(1,?,'excel-scope-other','e2e',0,1,'other')", 1L);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DROP TRIGGER IF EXISTS test_demo_item_excel_failure ON demo_item");
        jdbc.update("DROP FUNCTION IF EXISTS test_demo_item_excel_failure()");
        jdbc.update("DELETE FROM nz_idempotency WHERE scope LIKE '%/api/demo/item/import%'");
        jdbc.update("DELETE FROM demo_item WHERE name LIKE 'excel-scope-%' OR name LIKE 'excel-rollback-%' OR name='=1+1'");
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id IN (?,?)", ROLE_ID, LIMITED_ROLE_ID);
        jdbc.update("DELETE FROM sys_user_role WHERE user_id IN (?,?)", USER_ID, LIMITED_USER_ID);
        jdbc.update("DELETE FROM sys_user WHERE id IN (?,?)", USER_ID, LIMITED_USER_ID);
        jdbc.update("DELETE FROM sys_role WHERE id IN (?,?)", ROLE_ID, LIMITED_ROLE_ID);
    }

    @Test
    void importsAtomicallyWithServerOwnedScopeAndExportsOnlyVisibleRows() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);
        headers.set("Idempotency-Key", "excel-import-" + System.nanoTime());
        List<Map<String, Object>> valid = List.of(Map.of("name", "excel-scope-imported", "category", "e2e", "status", 0, "sort", 5, "remark", "owned by server"));
        JsonNode limitedLogin = rest.postForObject("/api/auth/login", Map.of("tenantCode", "default", "clientId", "nz-web-account", "username", "excel-query", "password", "test-password"), JsonNode.class);
        HttpHeaders limitedHeaders = new HttpHeaders();
        limitedHeaders.setContentType(MediaType.APPLICATION_JSON);
        limitedHeaders.set("Authorization", limitedLogin.path("data").asText());
        limitedHeaders.set("Idempotency-Key", "excel-denied-" + System.nanoTime());
        JsonNode denied = rest.exchange("/api/demo/item/import", HttpMethod.POST, new HttpEntity<>(valid, limitedHeaders), JsonNode.class).getBody();
        assertThat(denied.path("code").asInt()).isNotEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM demo_item WHERE name='excel-scope-imported'", Long.class)).isZero();
        JsonNode imported = rest.exchange("/api/demo/item/import", HttpMethod.POST, new HttpEntity<>(valid, headers), JsonNode.class).getBody();
        assertThat(imported.path("code").asInt()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT owner_id FROM demo_item WHERE name='excel-scope-imported'", Long.class)).isEqualTo(USER_ID);
        assertThat(jdbc.queryForObject("SELECT tenant_id FROM demo_item WHERE name='excel-scope-imported'", Long.class)).isEqualTo(1L);
        JsonNode replay = rest.exchange("/api/demo/item/import", HttpMethod.POST, new HttpEntity<>(valid, headers), JsonNode.class).getBody();
        assertThat(replay.path("code").asInt()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM demo_item WHERE name='excel-scope-imported'", Long.class)).isEqualTo(1L);
        headers.set("Idempotency-Key", "excel-invalid-" + System.nanoTime());
        List<Map<String, Object>> invalid = List.of(Map.of("name", "excel-scope-invalid", "category", "e2e", "status", 8, "sort", 5));
        JsonNode rejected = rest.exchange("/api/demo/item/import", HttpMethod.POST, new HttpEntity<>(invalid, headers), JsonNode.class).getBody();
        assertThat(rejected.path("code").asInt()).isNotEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM demo_item WHERE name='excel-scope-invalid'", Long.class)).isZero();

        jdbc.execute("CREATE FUNCTION test_demo_item_excel_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.name='excel-rollback-fail' THEN RAISE EXCEPTION 'injected insert failure'; END IF; RETURN NEW; END $$");
        jdbc.execute("CREATE TRIGGER test_demo_item_excel_failure BEFORE INSERT ON demo_item FOR EACH ROW EXECUTE FUNCTION test_demo_item_excel_failure()");
        headers.set("Idempotency-Key", "excel-rollback-" + System.nanoTime());
        List<Map<String, Object>> rollbackRows = List.of(
                Map.of("name", "excel-rollback-first", "category", "e2e", "status", 0, "sort", 5),
                Map.of("name", "excel-rollback-fail", "category", "e2e", "status", 0, "sort", 6));
        rest.exchange("/api/demo/item/import", HttpMethod.POST, new HttpEntity<>(rollbackRows, headers), JsonNode.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM demo_item WHERE name LIKE 'excel-rollback-%'", Long.class)).isZero();
        jdbc.execute("DROP TRIGGER test_demo_item_excel_failure ON demo_item");

        HttpHeaders downloadHeaders = new HttpHeaders();
        downloadHeaders.set("Authorization", token);
        byte[] workbook = rest.exchange("/api/demo/item/export?name=excel-scope-", HttpMethod.GET, new HttpEntity<>(downloadHeaders), byte[].class).getBody();
        List<?> rows = EasyExcel.read(new ByteArrayInputStream(workbook)).head(DemoItemExcelExportRow.class).sheet().doReadSync();
        assertThat(rows).hasSize(1);
    }
}
