package com.nz.admin.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

/** 仅管理本测试生成的 PostgreSQL schema，不清理共享数据库。 */
public final class PostgresTestSchema {
    private final String name;
    private boolean created;
    public PostgresTestSchema(String prefix) {
        if (!prefix.matches("[a-z_]{1,24}")) throw new IllegalArgumentException("Invalid test schema prefix");
        name = prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
    public String name() { return name; }
    public synchronized String url() {
        create();
        String base = System.getenv("NZ_TEST_PG_URL");
        return base + (base.contains("?") ? "&" : "?") + "currentSchema=" + name;
    }
    public void configure(DynamicPropertyRegistry registry) {
        create();
        registry.add("spring.datasource.url", this::url);
        registry.add("spring.datasource.hikari.schema", this::name);
        registry.add("spring.flyway.schemas", this::name);
        registry.add("spring.flyway.default-schema", this::name);
        registry.add("nz.job.scheduler-name", this::name);
    }
    private synchronized void create() {
        if (created) return;
        try (var connection = connect(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + name);
            created = true;
        } catch (SQLException error) { throw new IllegalStateException("Cannot create isolated test schema", error); }
    }
    public synchronized void drop() {
        if (!created) return;
        try (var connection = connect(); var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA " + name + " CASCADE");
            created = false;
        } catch (SQLException error) { throw new IllegalStateException("Cannot drop owned test schema " + name, error); }
    }
    private java.sql.Connection connect() throws SQLException {
        return DriverManager.getConnection(System.getenv("NZ_TEST_PG_URL"),
                System.getenv().getOrDefault("NZ_TEST_PG_USERNAME", "postgres"),
                System.getenv().getOrDefault("NZ_TEST_PG_PASSWORD", "postgres"));
    }
}
