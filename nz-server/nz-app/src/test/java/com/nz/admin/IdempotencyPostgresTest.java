package com.nz.admin;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.protection.core.IdempotencyExecutor;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** 使用独立 PostgreSQL 库验证真实事务与行锁。 */
@EnabledIfEnvironmentVariable(named = "NZ_TEST_PG_URL", matches = ".+")
class IdempotencyPostgresTest {
    private JdbcTemplate jdbc;
    private IdempotencyExecutor executor;
    private String scope;

    @BeforeEach
    void initialize() {
        var source =
                new DriverManagerDataSource(
                        System.getenv("NZ_TEST_PG_URL"),
                        System.getenv().getOrDefault("NZ_TEST_PG_USERNAME", "postgres"),
                        System.getenv().getOrDefault("NZ_TEST_PG_PASSWORD", "postgres"));
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(source);
        executor =
                new IdempotencyExecutor(
                        jdbc, new DataSourceTransactionManager(source), new ObjectMapper());
        scope = "integration:" + UUID.randomUUID();
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM nz_idempotency WHERE scope=?", scope);
    }

    @Test
    void concurrentRequestsCommitBusinessMutationOnlyOnce() throws Exception {
        var pool = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        var calls = new AtomicInteger();
        var futures = new ArrayList<Future<String>>();
        try {
            for (int i = 0; i < 16; i++)
                futures.add(
                        pool.submit(
                                () -> {
                                    start.await();
                                    return executor.execute(
                                            scope,
                                            "same",
                                            "hash",
                                            String.class,
                                            Duration.ofMinutes(1),
                                            () -> {
                                                calls.incrementAndGet();
                                                // 同一个事务中的第二条业务记录，用于验证数据库效果。
                                                jdbc.update(
                                                        "INSERT INTO"
                                                            + " nz_idempotency(scope,request_key,request_hash,expires_at)"
                                                            + " VALUES(?, 'effect', 'hash',"
                                                            + " CURRENT_TIMESTAMP + INTERVAL '1"
                                                            + " minute')",
                                                        scope);
                                                return "created";
                                            });
                                }));
            start.countDown();
            for (var future : futures)
                assertThat(future.get(30, TimeUnit.SECONDS)).isEqualTo("created");
            assertThat(calls).hasValue(1);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM nz_idempotency WHERE scope=?",
                                    Integer.class,
                                    scope))
                    .isEqualTo(2);
            assertThatThrownBy(
                            () ->
                                    executor.execute(
                                            scope,
                                            "same",
                                            "changed",
                                            String.class,
                                            Duration.ofMinutes(1),
                                            () -> "wrong"))
                    .isInstanceOf(BusinessException.class);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void failureRollsBackBusinessAndReservationThenAllowsRetry() {
        assertThatThrownBy(
                        () ->
                                executor.execute(
                                        scope,
                                        "retry",
                                        "hash",
                                        String.class,
                                        Duration.ofMinutes(1),
                                        () -> {
                                            jdbc.update(
                                                    "INSERT INTO"
                                                        + " nz_idempotency(scope,request_key,request_hash,expires_at)"
                                                        + " VALUES(?, 'effect', 'hash',"
                                                        + " CURRENT_TIMESTAMP + INTERVAL '1"
                                                        + " minute')",
                                                    scope);
                                            throw new IllegalStateException("business failed");
                                        }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM nz_idempotency WHERE scope=?",
                                Integer.class,
                                scope))
                .isZero();
        assertThat(
                        executor.execute(
                                scope,
                                "retry",
                                "hash",
                                String.class,
                                Duration.ofMinutes(1),
                                () -> "retried"))
                .isEqualTo("retried");
        jdbc.update(
                "UPDATE nz_idempotency SET expires_at=CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE"
                    + " scope=?",
                scope);
        assertThat(
                        executor.execute(
                                scope,
                                "retry",
                                "new hash",
                                String.class,
                                Duration.ofMinutes(1),
                                () -> "renewed"))
                .isEqualTo("renewed");
    }
}
