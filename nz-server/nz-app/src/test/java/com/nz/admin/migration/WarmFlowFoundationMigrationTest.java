package com.nz.admin.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class WarmFlowFoundationMigrationTest {

    @Test
    void preservesLegacyDataAndCreatesWarmFlowTables() throws Exception {
        String url = "jdbc:h2:mem:warm-flow-v25;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "NON_KEYWORDS=TYPE,VALUE,VERSION";

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            createLegacyTables(statement);
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V25__warm_flow_foundation.sql")
            );

            assertThat(queryCount(statement, "nz_flow_definition_legacy")).isEqualTo(1);
            assertThat(queryCount(statement, "nz_flow_instance_legacy")).isEqualTo(1);
            assertThat(queryCount(statement, "nz_flow_task_legacy")).isEqualTo(1);

            assertThat(queryCount(statement, "flow_definition")).isZero();
            assertThat(queryCount(statement, "flow_node")).isZero();
            assertThat(queryCount(statement, "flow_skip")).isZero();
            assertThat(queryCount(statement, "flow_instance")).isZero();
            assertThat(queryCount(statement, "flow_task")).isZero();
            assertThat(queryCount(statement, "flow_his_task")).isZero();
            assertThat(queryCount(statement, "flow_user")).isZero();
        }
    }

    private void createLegacyTables(Statement statement) throws Exception {
        statement.execute("""
                CREATE TABLE flow_definition (
                    definition_id BIGINT,
                    CONSTRAINT flow_definition_pkey PRIMARY KEY (definition_id)
                )
                """);
        statement.execute("""
                CREATE TABLE flow_instance (
                    instance_id BIGINT,
                    CONSTRAINT flow_instance_pkey PRIMARY KEY (instance_id)
                )
                """);
        statement.execute("""
                CREATE TABLE flow_task (
                    task_id BIGINT,
                    CONSTRAINT flow_task_pkey PRIMARY KEY (task_id)
                )
                """);
        statement.execute("INSERT INTO flow_definition (definition_id) VALUES (1)");
        statement.execute("INSERT INTO flow_instance (instance_id) VALUES (1)");
        statement.execute("INSERT INTO flow_task (task_id) VALUES (1)");
    }

    private long queryCount(Statement statement, String tableName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getLong(1);
        }
    }
}
