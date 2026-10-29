package com.nz.admin.modules.system.config;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.core.identity.IdentitySnapshot;
import com.nz.admin.common.core.identity.IdentityValidator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

import java.time.LocalDateTime;

/** HTTP 与长连接使用同一个数据库身份边界，修改回滚时版本也一起回滚。 */
@Component
public class SystemIdentityValidator implements IdentityValidator {
    private final JdbcTemplate jdbc;
    private final CommittedSecurityReader committed;

    public SystemIdentityValidator(JdbcTemplate jdbc, CommittedSecurityReader committed) {
        this.jdbc = jdbc;
        this.committed = committed;
    }

    @Override
    public boolean isValid(IdentitySnapshot identity) {
        if (identity == null || !identity.isComplete()) return false;
        try {
            return Boolean.TRUE.equals(committed.read(() -> jdbc.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM sys_user u JOIN sys_tenant t ON t.id=u.tenant_id
                    WHERE u.id=? AND u.tenant_id=? AND u.status=0 AND t.status=0
                    AND u.auth_version=? AND t.auth_version=?
                    AND (t.expire_time IS NULL OR t.expire_time>?))
                    """, Boolean.class, identity.userId(), identity.tenantId(), identity.userVersion(),
                    identity.tenantVersion(), LocalDateTime.now())));
        } catch (DataAccessException | TransactionException error) {
            var failure = new BusinessException(503, "身份状态暂不可用，请稍后重试");
            failure.initCause(error);
            throw failure;
        }
    }
}
