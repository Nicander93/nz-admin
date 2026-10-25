package com.nz.admin;

import cn.hutool.crypto.digest.BCrypt;
import com.fasterxml.jackson.databind.JsonNode;
import com.nz.admin.framework.datascope.core.DataScopeContext;
import com.nz.admin.framework.file.FileStorageProperties;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.file.FileDO;
import com.nz.admin.modules.system.service.file.FileService;
import com.nz.admin.modules.system.service.file.FileServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/** 真实文件、PostgreSQL 事务、HTTP 权限和可重试补偿闭环。 */
@EnabledIfEnvironmentVariable(named = "NZ_TEST_PG_URL", matches = ".+")
@SpringBootTest(classes = NzAdminApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"nz.cluster.enabled=false", "nz.redis.enabled=false", "nz.file.cleanup-interval-ms=3600000"})
@ActiveProfiles("pg-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileLifecyclePostgresTest {
    private static final long A = 980500L, B = 980501L;
    private static final long USER_A = 980510L, USER_B = 980511L, DENIED = 980512L;
    private static final long ROLE_A = 980520L, ROLE_B = 980521L, PACKAGE = 980530L;
    private static final String PASSWORD = "file-lifecycle-password";
    private static final Path ROOT = createRoot();
    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private final String tenantA = "files-a-" + suffix, tenantB = "files-b-" + suffix;
    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired FileService files;
    @Autowired FileServiceImpl recovery;
    @Autowired TransactionTemplate transactions;
    @Autowired FileStorageProperties defaults;
    private Session a, b, denied;

    @DynamicPropertySource
    static void localStorage(DynamicPropertyRegistry registry) {
        registry.add("nz.file.base-path", () -> ROOT.resolve("default").toString());
        registry.add("nz.cache.key-prefix", () -> "file-lifecycle-" + ROOT.getFileName());
    }

    @BeforeAll
    void fixtures() {
        cleanupRows();
        jdbc.update("INSERT INTO sys_tenant_package(id,package_name,status) VALUES(?,?,0)", PACKAGE, "files-" + suffix);
        jdbc.update("INSERT INTO sys_tenant_package_menu(package_id,menu_id) SELECT ?,id FROM sys_menu "
                + "WHERE perm LIKE 'system:file:%' OR perm LIKE 'system:fileconfig:%'", PACKAGE);
        for (long tenant : List.of(A, B)) {
            jdbc.update("INSERT INTO sys_tenant(id,tenant_code,tenant_name,package_id,account_count,status) VALUES(?,?,?,?,100,0)",
                    tenant, tenant == A ? tenantA : tenantB, "File Test Tenant", PACKAGE);
            long role = tenant == A ? ROLE_A : ROLE_B;
            jdbc.update("INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope) VALUES(?,?,?,?,0,0,1)",
                    role, tenant, "File Test Role", "files-" + tenant + "-" + suffix);
            jdbc.update("INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) SELECT ?,?,id FROM sys_menu "
                    + "WHERE perm LIKE 'system:file:%' OR perm LIKE 'system:fileconfig:%'", tenant, role);
        }
        insertUser(USER_A, A); insertUser(USER_B, B); insertUser(DENIED, A);
        jdbc.update("INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES(?,?,?),(?,?,?)", A, USER_A, ROLE_A, B, USER_B, ROLE_B);
        a = login(tenantA, USER_A); pauseLoginWindow();
        b = login(tenantB, USER_B); pauseLoginWindow();
        denied = login(tenantA, DENIED);
    }

    @BeforeEach
    void freshFiles() throws IOException {
        dropTriggers();
        jdbc.update("DELETE FROM sys_file WHERE tenant_id IN (?,?)", A, B);
        jdbc.update("DELETE FROM sys_file_config WHERE tenant_id IN (?,?)", A, B);
        clearFiles();
    }

    @AfterAll
    void cleanup() throws IOException {
        try {
            dropTriggers();
            if (a != null) a.exchange(HttpMethod.POST, "/api/auth/logout", null);
            if (b != null) b.exchange(HttpMethod.POST, "/api/auth/logout", null);
            if (denied != null) denied.exchange(HttpMethod.POST, "/api/auth/logout", null);
        } finally { cleanupRows(); clearFiles(); Files.deleteIfExists(ROOT); }
    }

    @Test
    void isolatesTenantConfigsAndKeepsOldFilesAfterSameTypeConfigChanges() throws IOException {
        long configA = config(a, "a-one", ROOT.resolve("a-one"));
        config(b, "b-one", ROOT.resolve("b-one"));
        JsonNode first = upload(a, "first.txt", "old location");
        JsonNode other = upload(b, "other.txt", "tenant B");
        assertThat(first.has("storageConfigSnapshot")).isFalse();
        assertThat(first.has("tenantId")).isFalse();
        assertThat(Path.of(first.path("filePath").asText())).startsWith(ROOT.resolve("a-one"));
        assertThat(Path.of(other.path("filePath").asText())).startsWith(ROOT.resolve("b-one"));
        Map<String, Object> update = configBody("a-two", ROOT.resolve("a-two")); update.put("id", configA);
        success(a.exchange(HttpMethod.PUT, "/api/system/file-config", update));
        JsonNode second = upload(a, "second.txt", "new location");
        assertThat(Path.of(second.path("filePath").asText())).startsWith(ROOT.resolve("a-two"));
        assertThat(download(a, first.path("id").asLong()).getBody()).isEqualTo("old location".getBytes());
        assertThat(download(a, second.path("id").asLong()).getBody()).isEqualTo("new location".getBytes());
        assertThat(download(b, other.path("id").asLong()).getBody()).isEqualTo("tenant B".getBytes());
        assertThat(defaults.getBasePath()).isEqualTo(ROOT.resolve("default").toString());
        assertThat(count("SELECT count(*) FROM sys_file_config WHERE tenant_id IN (?,?) AND status=0", A, B)).isEqualTo(2);
    }

    @Test
    void rejectsForeignFileAccessAndMissingPermissionsWithoutChangingObjects() throws IOException {
        JsonNode own = upload(a, "private.txt", "private content"); long id = own.path("id").asLong();
        assertThat(download(b, id).getStatusCode().value()).isEqualTo(404);
        success(b.exchange(HttpMethod.DELETE, "/api/system/file/" + id, null));
        assertThat(count("SELECT count(*) FROM sys_file WHERE id=?", id)).isEqualTo(1);
        assertThat(Files.readString(Path.of(own.path("filePath").asText()))).isEqualTo("private content");
        JsonNode page = success(b.exchange(HttpMethod.GET, "/api/system/file/page", null)).path("data").path("records");
        assertThat(page).isEmpty();
        rejected(denied.multipart("/api/system/file/upload", "file", "denied.txt", "denied"));
        rejected(denied.exchange(HttpMethod.GET, "/api/system/file/page", null));
        assertThat(count("SELECT count(*) FROM sys_file WHERE tenant_id IN (?,?)", A, B)).isEqualTo(1);
    }

    @Test
    void compensatesOuterTransactionRollbackButKeepsCommittedUploads() throws IOException {
        FileDO committed = inTenant(() -> transactions.execute(status -> directUpload("committed.txt")));
        assertThat(Files.exists(Path.of(committed.getFilePath()))).isTrue();
        FileDO rolledBack = inTenant(() -> transactions.execute(status -> {
            FileDO result = directUpload("rolled-back.txt"); status.setRollbackOnly(); return result;
        }));
        assertThat(count("SELECT count(*) FROM sys_file WHERE id=?", rolledBack.getId())).isZero();
        assertThat(Files.exists(Path.of(rolledBack.getFilePath()))).isFalse();
        assertThat(download(a, committed.getId()).getBody()).isEqualTo("content".getBytes());
    }

    @Test
    void rollsBackFailedUrlUpdateAndSupportsPerItemBatchTransactions() throws IOException {
        jdbc.execute("CREATE FUNCTION file_test_update_" + suffix + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
                + "IF NEW.tenant_id=" + A + " AND NEW.original_name='zero-update.txt' AND NEW.lifecycle_status='READY' THEN RETURN NULL; END IF; "
                + "IF NEW.tenant_id=" + A + " AND NEW.original_name='url-failure.txt' AND NEW.lifecycle_status='READY' "
                + "THEN RAISE EXCEPTION 'injected file update failure'; END IF; RETURN NEW; END $$");
        jdbc.execute("CREATE TRIGGER file_test_update_" + suffix + " BEFORE UPDATE ON sys_file FOR EACH ROW EXECUTE FUNCTION file_test_update_" + suffix + "()");
        rejected(a.multipart("/api/system/file/upload", "file", "url-failure.txt", "fail"));
        rejected(a.multipart("/api/system/file/upload", "file", "zero-update.txt", "zero rows"));
        assertThat(count("SELECT count(*) FROM sys_file WHERE tenant_id=?", A)).isZero();
        assertThat(regularFiles()).isZero();
        MockMultipartFile good = new MockMultipartFile("files", "good.txt", "text/plain", "good".getBytes());
        MockMultipartFile bad = new MockMultipartFile("files", "url-failure.txt", "text/plain", "bad".getBytes());
        FileService.BatchUploadResult result = inTenant(() -> files.uploadBatch(new MockMultipartFile[]{good, bad}, USER_A));
        assertThat(result.getSuccessCount()).isEqualTo(1); assertThat(result.getFailCount()).isEqualTo(1);
        assertThat(result.getFailedFiles().get(0).getIndex()).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sys_file WHERE tenant_id=? AND lifecycle_status='READY'", A)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sys_file WHERE tenant_id=? AND lifecycle_status<>'READY'", A)).isZero();
        assertThat(regularFiles()).isEqualTo(1);
    }

    @Test
    void retainsDurableDeleteIntentWhenDatabaseDeleteFailsAndRetriesIdempotently() throws IOException {
        JsonNode file = upload(a, "delete-db.txt", "content"); long id = file.path("id").asLong();
        jdbc.execute("CREATE FUNCTION file_test_delete_" + suffix + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
                + "IF OLD.id=" + id + " THEN RAISE EXCEPTION 'injected file delete failure'; END IF; RETURN OLD; END $$");
        jdbc.execute("CREATE TRIGGER file_test_delete_" + suffix + " BEFORE DELETE ON sys_file FOR EACH ROW EXECUTE FUNCTION file_test_delete_" + suffix + "()");
        rejected(a.exchange(HttpMethod.DELETE, "/api/system/file/" + id, null));
        assertThat(Files.exists(Path.of(file.path("filePath").asText()))).isFalse();
        assertThat(jdbc.queryForObject("SELECT lifecycle_status FROM sys_file WHERE id=?", String.class, id)).isEqualTo("DELETE_PENDING");
        assertThat(jdbc.queryForObject("SELECT cleanup_attempts FROM sys_file WHERE id=?", Integer.class, id)).isEqualTo(1);
        assertThat(download(a, id).getStatusCode().value()).isEqualTo(404);
        dropTriggers();
        success(a.exchange(HttpMethod.DELETE, "/api/system/file/" + id, null));
        success(a.exchange(HttpMethod.DELETE, "/api/system/file/" + id, null));
        assertThat(count("SELECT count(*) FROM sys_file WHERE id=?", id)).isZero();
    }

    @Test
    void neverRecursivelyDeletesDirectoryAndRecoversAfterStorageFailure() throws IOException {
        JsonNode file = upload(a, "delete-storage.txt", "content"); long id = file.path("id").asLong();
        Path path = Path.of(file.path("filePath").asText()); Files.delete(path); Files.createDirectory(path);
        Path protectedChild = Files.writeString(path.resolve("must-survive.txt"), "preserved");
        rejected(a.exchange(HttpMethod.DELETE, "/api/system/file/" + id, null));
        assertThat(Files.readString(protectedChild)).isEqualTo("preserved");
        assertThat(jdbc.queryForObject("SELECT lifecycle_status FROM sys_file WHERE id=?", String.class, id)).isEqualTo("DELETE_PENDING");
        Files.delete(protectedChild); Files.delete(path);
        jdbc.update("UPDATE sys_file SET cleanup_after=? WHERE id=?", LocalDateTime.now().minusMinutes(1), id);
        recovery.recoverPendingFiles();
        assertThat(count("SELECT count(*) FROM sys_file WHERE id=?", id)).isZero();
    }

    @Test
    void recoversCrashResidueAndSkipsRowsHeldByLiveTransactions() throws IOException {
        JsonNode file = upload(a, "crash.txt", "content"); long id = file.path("id").asLong();
        jdbc.update("UPDATE sys_file SET lifecycle_status='UPLOADING', cleanup_after=? WHERE id=?", LocalDateTime.now().minusHours(1), id);
        transactions.executeWithoutResult(status -> {
            jdbc.queryForList("SELECT id FROM sys_file WHERE id=? FOR UPDATE", id);
            java.util.concurrent.CompletableFuture.runAsync(recovery::recoverPendingFiles).join();
            assertThat(count("SELECT count(*) FROM sys_file WHERE id=?", id)).isEqualTo(1);
            assertThat(Files.exists(Path.of(file.path("filePath").asText()))).isTrue();
        });
        recovery.recoverPendingFiles();
        assertThat(count("SELECT count(*) FROM sys_file WHERE id=?", id)).isZero();
        assertThat(Files.exists(Path.of(file.path("filePath").asText()))).isFalse();
    }

    @Test
    void requiresExplicitLegacyBindingAndNeverRebindsExistingFiles() throws IOException {
        long ownConfig = config(a, "legacy", ROOT.resolve("legacy"));
        long foreignConfig = config(b, "foreign", ROOT.resolve("foreign"));
        Path path = ROOT.resolve("legacy/original.txt"); Files.createDirectories(path.getParent()); Files.writeString(path, "legacy");
        long id = jdbc.queryForObject("INSERT INTO sys_file(tenant_id,file_name,original_name,file_path,file_size,storage_type) "
                + "VALUES(?, 'original.txt','original.txt',?,6,'local') RETURNING id", Long.class, A, path.toString());
        assertThat(download(a, id).getHeaders().getContentType().toString()).contains("application/json");
        rejected(a.exchange(HttpMethod.PUT, "/api/system/file/" + id + "/storage-binding/" + foreignConfig, null));
        success(a.exchange(HttpMethod.PUT, "/api/system/file/" + id + "/storage-binding/" + ownConfig, null));
        assertThat(download(a, id).getBody()).isEqualTo("legacy".getBytes());
        rejected(a.exchange(HttpMethod.PUT, "/api/system/file/" + id + "/storage-binding/" + ownConfig, null));
    }

    @Test
    void upgradesRealV32FileRowsWithoutLosingLocationsOrGuessingConfig() {
        String schema = "file_upgrade_" + suffix;
        try {
            org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .target("32").load().migrate();
            jdbc.update("INSERT INTO " + schema + ".sys_file(file_name,original_name,file_suffix,file_size,file_path,url,storage_type) "
                    + "VALUES('legacy.txt','legacy.txt','txt',6,'/old/location/legacy.txt','/old/download/1',1)");
            org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .load().migrate();
            Map<String,Object> row = jdbc.queryForMap("SELECT * FROM " + schema + ".sys_file WHERE file_name='legacy.txt'");
            assertThat(row.get("file_ext")).isEqualTo("txt");
            assertThat(row.get("file_url")).isEqualTo("/old/download/1");
            assertThat(row.get("file_path")).isEqualTo("/old/location/legacy.txt");
            assertThat(row.get("storage_type")).isEqualTo("local");
            assertThat(row.get("lifecycle_status")).isEqualTo("READY");
            assertThat(row.get("storage_config_snapshot")).isNull();
        } finally {
            jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    private long config(Session session, String name, Path path) {
        long id = success(session.exchange(HttpMethod.POST, "/api/system/file-config", configBody(name, path))).path("data").asLong();
        success(session.exchange(HttpMethod.POST, "/api/system/file-config/" + id + "/test", null));
        success(session.exchange(HttpMethod.PUT, "/api/system/file-config/" + id + "/activate", null));
        return id;
    }
    private Map<String, Object> configBody(String name, Path path) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("configName", name); body.put("storageType", "local"); body.put("basePath", path.toString());
        body.put("maxFileSizeBytes", 1048576); body.put("localAccessUrlPrefix", "/api/system/file/download/");
        return body;
    }
    private JsonNode upload(Session session, String name, String content) {
        return success(session.multipart("/api/system/file/upload", "file", name, content)).path("data");
    }
    private FileDO directUpload(String name) {
        try { return files.upload(new MockMultipartFile("file", name, "text/plain", "content".getBytes()), USER_A); }
        catch (IOException exception) { throw new UncheckedIOException(exception); }
    }
    private ResponseEntity<byte[]> download(Session session, long id) {
        HttpHeaders headers = new HttpHeaders(); headers.set("Authorization", session.token);
        return rest.exchange("/api/system/file/download/" + id, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }
    private <T> T inTenant(java.util.function.Supplier<T> action) {
        return TenantContextHolder.callWithTenantId(A, () -> DataScopeContext.withoutFilter(action));
    }
    private void insertUser(long id, long tenant) {
        jdbc.update("INSERT INTO sys_user(id,tenant_id,username,password,nickname,status) VALUES(?,?,?,?,?,0)",
                id, tenant, "file-user-" + id + "-" + suffix, BCrypt.hashpw(PASSWORD), "File User");
    }
    private Session login(String tenant, long id) {
        JsonNode response = success(rest.postForEntity("/api/auth/login", Map.of("tenantCode", tenant,
                "clientId", "nz-web-account", "username", "file-user-" + id + "-" + suffix, "password", PASSWORD), JsonNode.class));
        return new Session(response.path("data").asText());
    }
    private void pauseLoginWindow() {
        try { Thread.sleep(1050); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
    }
    private JsonNode success(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(); assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asInt()).as(response.getBody().toString()).isEqualTo(200); return response.getBody();
    }
    private void rejected(ResponseEntity<JsonNode> response) {
        assertThat(!response.getStatusCode().is2xxSuccessful() || response.getBody().path("code").asInt()!=200).isTrue();
    }
    private long count(String sql, Object... args) { return jdbc.queryForObject(sql, Long.class, args); }
    private long regularFiles() throws IOException { try (var stream = Files.walk(ROOT)) { return stream.filter(Files::isRegularFile).count(); } }
    private void dropTriggers() {
        for (String kind : List.of("update", "delete")) {
            jdbc.execute("DROP TRIGGER IF EXISTS file_test_" + kind + "_" + suffix + " ON sys_file");
            jdbc.execute("DROP FUNCTION IF EXISTS file_test_" + kind + "_" + suffix + "()");
        }
    }
    private void cleanupRows() {
        jdbc.update("DELETE FROM sys_file WHERE tenant_id IN (?,?)", A, B);
        jdbc.update("DELETE FROM sys_file_config WHERE tenant_id IN (?,?)", A, B);
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id IN (?,?)", ROLE_A, ROLE_B);
        jdbc.update("DELETE FROM sys_user_role WHERE user_id IN (?,?,?)", USER_A, USER_B, DENIED);
        jdbc.update("DELETE FROM sys_user WHERE id IN (?,?,?)", USER_A, USER_B, DENIED);
        jdbc.update("DELETE FROM sys_role WHERE id IN (?,?)", ROLE_A, ROLE_B);
        jdbc.update("DELETE FROM sys_tenant WHERE id IN (?,?)", A, B);
        jdbc.update("DELETE FROM sys_tenant_package_menu WHERE package_id=?", PACKAGE);
        jdbc.update("DELETE FROM sys_tenant_package WHERE id=?", PACKAGE);
    }
    private static Path createRoot() {
        try { return Files.createTempDirectory("nz-file-lifecycle-"); } catch (IOException exception) { throw new UncheckedIOException(exception); }
    }
    private void clearFiles() throws IOException {
        try (var paths = Files.walk(ROOT)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) if (!path.equals(ROOT)) Files.deleteIfExists(path);
        }
    }
    private final class Session {
        final String token;
        Session(String token) { this.token = token; }
        ResponseEntity<JsonNode> exchange(HttpMethod method, String path, Object body) {
            HttpHeaders headers = new HttpHeaders(); headers.set("Authorization", token);
            return rest.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
        }
        ResponseEntity<JsonNode> multipart(String path, String field, String name, String content) {
            var body = new LinkedMultiValueMap<String,Object>();
            var resource = new ByteArrayResource(content.getBytes()) { @Override public String getFilename() { return name; } };
            HttpHeaders part = new HttpHeaders(); part.setContentType(MediaType.TEXT_PLAIN);
            body.add(field, new HttpEntity<>(resource, part));
            HttpHeaders headers = new HttpHeaders(); headers.set("Authorization", token); headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
        }
    }
}
