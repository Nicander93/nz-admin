package com.nz.admin.framework.protection.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nz.admin.common.core.BusinessException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/** PostgreSQL 行锁串行化同一业务请求，数据库变更和缓存响应在同一事务中提交。 */
public class IdempotencyExecutor {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper mapper;

    public IdempotencyExecutor(
            JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(manager);
        this.mapper = mapper;
    }

    public <T> T execute(
            String scope,
            String key,
            String fingerprint,
            Type type,
            Duration retention,
            Supplier<T> action) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,128}") || retention.toSeconds() < 1)
            throw new BusinessException("幂等键或保留时间不合法");
        return transactions.execute(
                status -> {
                    Instant expiry = Instant.now().plus(retention);
                    jdbc.update(
                            "INSERT INTO nz_idempotency(scope,request_key,request_hash,expires_at)"
                                + " VALUES(?,?,?,?) ON CONFLICT(scope,request_key) DO NOTHING",
                            scope,
                            key,
                            fingerprint,
                            java.time.OffsetDateTime.ofInstant(expiry, java.time.ZoneOffset.UTC));
                    var record =
                            jdbc.queryForObject(
                                    "SELECT request_hash,response_json,expires_at FROM"
                                        + " nz_idempotency WHERE scope=? AND request_key=? FOR"
                                        + " UPDATE",
                                    (rs, row) ->
                                            new Entry(
                                                    rs.getString(1),
                                                    rs.getString(2),
                                                    rs.getObject(3, java.time.OffsetDateTime.class)
                                                            .toInstant()),
                                    scope,
                                    key);
                    if (record == null) throw new IllegalStateException("幂等记录未创建");
                    boolean expired = !record.expiry().isAfter(Instant.now());
                    if (!expired && !fingerprint.equals(record.hash()))
                        throw new BusinessException(409, "同一幂等键不能用于不同请求");
                    try {
                        if (!expired && record.response() != null)
                            return mapper.readValue(record.response(), mapper.constructType(type));
                        T result = action.get();
                        jdbc.update(
                                "UPDATE nz_idempotency SET"
                                    + " request_hash=?,response_json=?,expires_at=? WHERE scope=?"
                                    + " AND request_key=?",
                                fingerprint,
                                mapper.writeValueAsString(result),
                                java.time.OffsetDateTime.ofInstant(
                                        Instant.now().plus(retention), java.time.ZoneOffset.UTC),
                                scope,
                                key);
                        return result;
                    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                        throw new IllegalStateException("幂等响应无法序列化", e);
                    }
                });
    }

    /** 可通过任务模块定期调用，已过期结果不再保证重放。 */
    public int purgeExpired() {
        return jdbc.update("DELETE FROM nz_idempotency WHERE expires_at < CURRENT_TIMESTAMP");
    }

    private record Entry(String hash, String response, Instant expiry) {}
}
