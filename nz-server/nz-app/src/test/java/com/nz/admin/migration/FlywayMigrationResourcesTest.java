package com.nz.admin.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationResourcesTest {

    private static final List<String> MIGRATIONS = List.of(
            "db/migration/V1__baseline.sql",
            "db/migration/V2__menus.sql",
            "db/migration/V3__client_module.sql",
            "db/migration/V4__job_module.sql",
            "db/migration/V5__mail.sql",
            "db/migration/V6__file_config.sql",
            "db/migration/V7__demo_module.sql",
            "db/migration/V8__generator_module.sql",
            "db/migration/V9__tenant_management.sql",
            "db/migration/V10__field_encryption.sql",
            "db/migration/V11__s3_storage_delivery.sql",
            "db/migration/V12__online_user_management.sql",
            "db/migration/V13__realtime_communication.sql",
            "db/migration/V14__sms_management.sql",
            "db/migration/V15__sms_login.sql",
            "db/migration/V16__social_login.sql",
            "db/migration/V17__message_center.sql",
            "db/migration/V18__user_profile.sql",
            "db/migration/V19__workflow_category.sql",
            "db/migration/V20__workflow_definition.sql",
            "db/migration/V21__workflow_instance.sql",
            "db/migration/V24__workflow_instance_urge.sql",
            "db/migration/V22__workflow_task.sql",
            "db/migration/V23__workflow_task_delegate.sql"
    );

    @Test
    void providesContiguousVersionedMigrations() {
        assertThat(MIGRATIONS).allSatisfy(path ->
                assertThat(getClass().getClassLoader().getResource(path)).as(path).isNotNull());
    }

    @Test
    void baselineAndModuleMigrationsKeepRequiredCapabilities() throws IOException {
        assertThat(read(MIGRATIONS.get(0))).contains("CREATE TABLE IF NOT EXISTS sys_user");
        assertThat(read(MIGRATIONS.get(2))).contains("CREATE TABLE IF NOT EXISTS sys_client");
        assertThat(read(MIGRATIONS.get(3))).contains("job/index");
        assertThat(read(MIGRATIONS.get(4))).contains("system:mail:test");
        assertThat(read(MIGRATIONS.get(5))).contains(
                "CREATE TABLE IF NOT EXISTS sys_file_config",
                "system:fileconfig:list"
        );
        assertThat(read(MIGRATIONS.get(6))).contains(
                "CREATE TABLE IF NOT EXISTS demo_item",
                "demo/item/index",
                "demo:item:list",
                "demo:item:add",
                "demo:item:edit",
                "demo:item:remove"
        );
        assertThat(read(MIGRATIONS.get(7))).contains(
                "generator/index",
                "generator:table:list",
                "generator:table:query",
                "generator:table:preview",
                "generator:table:download"
        );
        assertThat(read(MIGRATIONS.get(8))).contains(
                "CREATE TABLE IF NOT EXISTS sys_tenant",
                "CREATE TABLE IF NOT EXISTS sys_tenant_package",
                "ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS tenant_id",
                "uk_sys_user_tenant_username",
                "system:tenant:list",
                "system:tenantpackage:list"
        );
        assertThat(read(MIGRATIONS.get(9))).contains(
                "ALTER COLUMN email TYPE VARCHAR(512)",
                "system:user:contact:plain",
                "system:user:contact:encrypt"
        );
        assertThat(read(MIGRATIONS.get(10))).contains(
                "ADD COLUMN IF NOT EXISTS region",
                "storage_type IN ('local', 'oss', 's3')",
                "system:fileconfig:test"
        );
        assertThat(read(MIGRATIONS.get(11))).contains(
                "system/online/index",
                "system:online:force"
        );
        assertThat(read(MIGRATIONS.get(12))).contains(
                "system/realtime/index",
                "system:realtime:send"
        );
        assertThat(read(MIGRATIONS.get(13))).contains(
                "CREATE TABLE IF NOT EXISTS sys_sms_channel",
                "CREATE TABLE IF NOT EXISTS sys_sms_template",
                "CREATE TABLE IF NOT EXISTS sys_sms_send_log",
                "system/sms/index", "system:sms:send"
        );
        assertThat(read(MIGRATIONS.get(14))).contains(
                "phone_hash",
                "nz-web-account",
                "nz-web-sms"
        );

        assertThat(read(MIGRATIONS.get(15))).contains(
                "CREATE TABLE IF NOT EXISTS sys_social",
                "nz-web-social",
                "system:social:bind"
        );
        assertThat(read(MIGRATIONS.get(16))).contains(
                "CREATE TABLE IF NOT EXISTS sys_message",
                "read_status",
                "system/message/index",
                "system:message:send"
        );
        assertThat(read(MIGRATIONS.get(17))).contains(
                "ADD COLUMN IF NOT EXISTS gender",
                "ADD COLUMN IF NOT EXISTS avatar_file_id",
                "chk_sys_user_gender"
        );
        assertThat(read(MIGRATIONS.get(18))).contains(
                "CREATE TABLE IF NOT EXISTS flow_category",
                "uk_flow_category_tenant_parent_name",
                "workflow/category/index",
                "workflow:category:list",
                "workflow:category:export"
        );
        assertThat(read(MIGRATIONS.get(19))).contains(
                "CREATE TABLE IF NOT EXISTS flow_definition",
                "uk_flow_definition_tenant_code_version",
                "uk_flow_definition_tenant_code_published",
                "workflow/definition/index",
                "workflow:definition:publish",
                "workflow:definition:export"
        );
        assertThat(read(MIGRATIONS.get(20))).contains(
                "CREATE TABLE IF NOT EXISTS flow_instance",
                "CREATE TABLE IF NOT EXISTS flow_instance_event",
                "uk_flow_instance_tenant_business",
                "workflow/instance/index",
                "workflow:instance:start",
                "workflow:instance:action"
        );
        assertThat(read(MIGRATIONS.get(21))).contains(
                "CREATE TABLE IF NOT EXISTS flow_task",
                "CREATE TABLE IF NOT EXISTS flow_history_task",
                "CREATE TABLE IF NOT EXISTS flow_task_copy",
                "INSERT INTO flow_task",
                "workflow/task/index",
                "workflow:task:action",
                "workflow:task:transfer",
                "workflow:task:copy"
        );
        assertThat(read(MIGRATIONS.get(22))).contains(
                "ADD COLUMN IF NOT EXISTS owner_assignee",
        assertThat(read(MIGRATIONS.get(23))).contains(
                "workflow:instance:urge",
                "INSERT INTO sys_role_menu",
                "INSERT INTO sys_tenant_package_menu"
        );
                "ADD COLUMN IF NOT EXISTS delegation_status",
                "'DELEGATE', 'RESOLVE'",
                "workflow:task:delegate"
        );
    }

    private String read(String path) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as(path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
