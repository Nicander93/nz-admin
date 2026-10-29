package com.nz.admin.modules.job.service.job;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.job.*;
import com.nz.admin.framework.quartz.config.JobSchedulerProperties;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.job.entity.dataobject.job.JobDO;
import com.nz.admin.modules.job.entity.dataobject.job.JobExecutionDO;
import com.nz.admin.modules.job.entity.vo.job.JobExecutionReceipt;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/** 先持久认领，再将业务写入、成功日志和终态放进同一数据库事务。 */
@Service
public class JobExecutionService implements JobExecutionCoordinator {
    private static final BeanPropertyRowMapper<JobExecutionDO> ROW = BeanPropertyRowMapper.newInstance(JobExecutionDO.class);
    private static final BeanPropertyRowMapper<JobDO> JOB_ROW = BeanPropertyRowMapper.newInstance(JobDO.class);
    private static final String EXECUTIONS_FROM = " FROM sys_job_execution execution";
    // 读模型和重试命令共用资格判断，配置已更新或删除的旧执行不能重放。
    private static final String EXECUTION_VIEW = """
            SELECT execution.*, (execution.state='FAILED'
                AND (execution.attempt=0 OR execution.recoverable)
                AND EXISTS(SELECT 1 FROM sys_job definition
                    WHERE definition.id=execution.job_id AND definition.tenant_id=execution.tenant_id
                    AND definition.revision=execution.definition_revision)) AS retryable
            """ + EXECUTIONS_FROM;
    private final JdbcTemplate jdbc;
    private final JobInvokeLogSink logs;
    private final JobTenantProvider tenants;
    private final JobSchedulerProperties options;
    private final TransactionTemplate transaction;
    private final boolean supported;

    public JobExecutionService(JdbcTemplate jdbc, JobTenantProvider tenants, JobSchedulerProperties options,
                               PlatformTransactionManager transactions, JobInvokeLogSink logs) throws SQLException {
        this.jdbc = jdbc;
        this.logs = logs;
        this.tenants = tenants;
        this.options = options;
        this.transaction = new TransactionTemplate(transactions);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        try (var connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            supported = "PostgreSQL".equals(connection.getMetaData().getDatabaseProductName());
        }
        if (options.getExecutionLease().toSeconds() < 5 || options.getMaxDispatchAttempts() < 1)
            throw new IllegalArgumentException("任务租约至少5秒，重试次数至少1次");
    }

    public boolean isSupported() { return supported; }
    @Override public boolean recordsFailures() { return true; }

    public JobExecutionReceipt enqueue(JobDO job, String requestKey, boolean recoverable) {
        requireSupported();
        Long tenant = currentTenant();
        if (!tenant.equals(job.getTenantId())) throw new BusinessException(403, "任务归属不正确");
        String key = StrUtil.isBlank(requestKey) ? IdUtil.fastSimpleUUID() : requestKey;
        if (!key.matches("[a-zA-Z0-9._:-]{1,128}")) throw new BusinessException("Idempotency-Key 格式不正确");
        key = "manual:" + tenant + ":" + job.getId() + ":" + key;
        JobExecutionDO existing = findByKey(tenant, key);
        if (existing != null) return receipt(existing);
        insert(job, key, "MANUAL", System.currentTimeMillis(), recoverable, false);
        return receipt(Objects.requireNonNull(findByKey(tenant, key)));
    }

    @Override
    public boolean execute(JobInvocation invocation, Runnable action) {
        requireSupported();
        Long tenant = currentTenant();
        JobDO current = definition(tenant, invocation.jobId());
        JobRunContext context = invocation.context();
        if (context == null) {
            if (current == null) return false;
            context = new JobRunContext(tenant, current.getRevision(), "legacy:" + tenant + ":" + invocation.jobId() + ":" + IdUtil.fastSimpleUUID(),
                    "MANUAL", System.currentTimeMillis(), false, ManagementFactory.getRuntimeMXBean().getName(), null, null, false);
        }
        if (!tenant.equals(context.tenantId())) throw new BusinessException(403, "任务执行租户不匹配");
        final JobRunContext requested = context;
        transaction.executeWithoutResult(status -> {
            if (findByKey(tenant, requested.executionKey()) == null && current != null) {
                insert(current, requested.executionKey(), requested.triggerSource(), requested.scheduledFireTime(),
                        requested.recoverable(), true);
            }
        });
        Claim claim = transaction.execute(status -> claim(invocation, requested));
        if (claim == null) return false;
        try {
            Boolean executed = transaction.execute(status -> {
                List<JobExecutionDO> rows = jdbc.query("SELECT * FROM sys_job_execution WHERE id=? AND owner_token=? AND state='RUNNING' FOR UPDATE SKIP LOCKED",
                        ROW, claim.execution().getId(), claim.owner());
                if (rows.isEmpty()) return false;
                JobExecutionDO row = rows.get(0);
                if (!validDefinition(row, invocation)) {
                    skip(row.getId(), "任务、租户或配置版本已失效");
                    return false;
                }
                JobRunContextHolder.runWith(claim.context(), action);
                jdbc.update("UPDATE sys_job_execution SET state='SUCCEEDED',end_time=CURRENT_TIMESTAMP,update_time=CURRENT_TIMESTAMP,lease_until=NULL,last_error=NULL WHERE id=? AND owner_token=?",
                        row.getId(), claim.owner());
                return true;
            });
            return Boolean.TRUE.equals(executed);
        } catch (RuntimeException failure) {
            try {
                transaction.executeWithoutResult(status -> {
                    int changed = jdbc.update("UPDATE sys_job_execution SET state='FAILED',last_error=?,end_time=CURRENT_TIMESTAMP,update_time=CURRENT_TIMESTAMP,lease_until=NULL WHERE id=? AND owner_token=? AND state='RUNNING'",
                            errorMessage(failure), claim.execution().getId(), claim.owner());
                    if (changed == 1) logs.save(JobInvokeLogEvent.from(invocation, claim.context()).setStatus(1)
                            .setJobMessage(StrUtil.subPre("执行失败：" + errorMessage(failure), 500)).setExceptionInfo(errorMessage(failure)));
                });
            } catch (RuntimeException recordingFailure) { failure.addSuppressed(recordingFailure); }
            throw failure;
        }
    }

    private Claim claim(JobInvocation invocation, JobRunContext requested) {
        List<JobExecutionDO> rows = jdbc.query("SELECT * FROM sys_job_execution WHERE scheduler_name=? AND tenant_id=? AND execution_key=? FOR UPDATE",
                ROW, options.getSchedulerName(), requested.tenantId(), requested.executionKey());
        if (rows.isEmpty()) return null;
        JobExecutionDO row = rows.get(0);
        if (List.of("SUCCEEDED", "FAILED", "SKIPPED").contains(row.getState())) return null;
        if ("RUNNING".equals(row.getState()) && (!requested.recovered() || !Boolean.TRUE.equals(row.getRecoverable()))) return null;
        if (!validDefinition(row, invocation)) {
            skip(row.getId(), "任务、租户或配置版本已失效");
            return null;
        }
        if ("RUNNING".equals(row.getState())) recordInterruptedAttempt(row, "恢复尝试接管，上一尝试未提交结果");
        String owner = IdUtil.fastSimpleUUID();
        int attempt = row.getAttempt() + 1;
        boolean recovered = requested.recovered() || Boolean.TRUE.equals(row.getRecovered());
        jdbc.update("UPDATE sys_job_execution SET state='RUNNING',attempt=?,owner_token=?,node_id=?,recovered=?,dispatched=TRUE,lease_until=CURRENT_TIMESTAMP+(?*INTERVAL '1 millisecond'),start_time=CURRENT_TIMESTAMP,end_time=NULL,last_error=NULL,update_time=CURRENT_TIMESTAMP WHERE id=?",
                attempt, owner, requested.nodeId(), recovered, options.getExecutionLease().toMillis(), row.getId());
        var active = new JobRunContext(row.getTenantId(), row.getDefinitionRevision(), row.getExecutionKey(),
                row.getTriggerSource(), row.getScheduledFireTime(), recovered, requested.nodeId(), row.getId(), attempt,
                Boolean.TRUE.equals(row.getRecoverable()));
        return new Claim(row, owner, active);
    }

    private boolean validDefinition(JobExecutionDO execution, JobInvocation invocation) {
        JobDO definition = definition(execution.getTenantId(), execution.getJobId());
        return definition != null && tenants.isEnabledTenant(execution.getTenantId())
                && Objects.equals(definition.getRevision(), execution.getDefinitionRevision())
                && Objects.equals(definition.getInvokeTarget(), invocation.invokeTarget())
                && (invocation.context() == null || Objects.equals(invocation.context().definitionRevision(), execution.getDefinitionRevision()))
                && (!"SCHEDULED".equals(execution.getTriggerSource()) || Integer.valueOf(0).equals(definition.getStatus()));
    }

    private void insert(JobDO job, String key, String source, Long scheduled, boolean recoverable, boolean dispatched) {
        jdbc.update("INSERT INTO sys_job_execution(scheduler_name,tenant_id,job_id,definition_revision,execution_key,trigger_source,scheduled_fire_time,snapshot,recoverable,dispatched) VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT(scheduler_name,tenant_id,execution_key) DO NOTHING",
                options.getSchedulerName(), job.getTenantId(), job.getId(), job.getRevision(), key, source, scheduled,
                JSONUtil.toJsonStr(job), recoverable, dispatched);
    }

    private void skip(Long id, String reason) {
        jdbc.update("UPDATE sys_job_execution SET state='SKIPPED',last_error=?,end_time=CURRENT_TIMESTAMP,lease_until=NULL,update_time=CURRENT_TIMESTAMP WHERE id=?", reason, id);
    }

    JobDO definition(Long tenant, Long id) {
        List<JobDO> rows = jdbc.query("SELECT * FROM sys_job WHERE id=? AND tenant_id=?", JOB_ROW, id, tenant);
        return rows.isEmpty() ? null : rows.get(0);
    }

    JobExecutionDO findByKey(Long tenant, String key) {
        List<JobExecutionDO> rows = jdbc.query("SELECT * FROM sys_job_execution WHERE scheduler_name=? AND tenant_id=? AND execution_key=?",
                ROW, options.getSchedulerName(), tenant, key);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public JobExecutionDO getCurrent(Long id) {
        requireSupported();
        List<JobExecutionDO> rows = jdbc.query(EXECUTION_VIEW + " WHERE id=? AND tenant_id=? AND scheduler_name=?",
                ROW, id, currentTenant(), options.getSchedulerName());
        if (rows.isEmpty()) throw new BusinessException("执行记录不存在或无权访问");
        return rows.get(0);
    }

    public Page<JobExecutionDO> list(int page, int size, Long jobId, String state, String key) {
        requireSupported();
        if (page < 1 || size < 1 || size > 200) throw new BusinessException("分页参数不正确");
        Long tenant = currentTenant();
        String filter = " WHERE scheduler_name=? AND tenant_id=?";
        var values = new java.util.ArrayList<Object>(List.of(options.getSchedulerName(), tenant));
        if (jobId != null) { filter += " AND job_id=?"; values.add(jobId); }
        if (StrUtil.isNotBlank(state)) { filter += " AND state=?"; values.add(state); }
        if (StrUtil.isNotBlank(key)) { filter += " AND execution_key=?"; values.add(key); }
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + EXECUTIONS_FROM + filter, Long.class, values.toArray());
        values.add(size); values.add((page - 1L) * size);
        List<JobExecutionDO> rows = jdbc.query(EXECUTION_VIEW + filter + " ORDER BY id DESC LIMIT ? OFFSET ?", ROW, values.toArray());
        return new Page<JobExecutionDO>(page, size, total == null ? 0 : total).setRecords(rows);
    }

    public JobExecutionReceipt retry(Long id) {
        requireSupported();
        Long tenant = currentTenant();
        return transaction.execute(status -> {
            List<JobExecutionDO> rows = jdbc.query(EXECUTION_VIEW + " WHERE id=? AND scheduler_name=? AND tenant_id=? FOR UPDATE",
                    ROW, id, options.getSchedulerName(), tenant);
            if (rows.isEmpty()) throw new BusinessException("执行记录不存在或无权访问");
            JobExecutionDO row = rows.get(0);
            if (!"FAILED".equals(row.getState())) return receipt(row);
            if (!row.isRetryable())
                throw new BusinessException("仅允许重试当前配置下尚未执行业务或明确支持幂等恢复的失败执行");
            jdbc.update("UPDATE sys_job_execution SET state='PENDING',dispatched=FALSE,dispatch_version=dispatch_version+1,dispatch_attempts=0,next_attempt_time=CURRENT_TIMESTAMP,owner_token=NULL,lease_until=NULL,recovered=TRUE,last_error=NULL,update_time=CURRENT_TIMESTAMP WHERE id=?", id);
            return new JobExecutionReceipt(id, row.getExecutionKey(), "PENDING");
        });
    }

    /** 活跃业务事务持有行锁；超期扫描不会把仍在运行的事务当成失联节点。 */
    boolean recoverOneExpired() {
        Boolean found = transaction.execute(status -> {
            List<JobExecutionDO> rows = jdbc.query("SELECT * FROM sys_job_execution WHERE scheduler_name=? AND state='RUNNING' AND lease_until<CURRENT_TIMESTAMP ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED",
                    ROW, options.getSchedulerName());
            if (rows.isEmpty()) return false;
            JobExecutionDO row = rows.get(0);
            recordInterruptedAttempt(row, "执行租约已过期，上一尝试未提交结果");
            if (Boolean.TRUE.equals(row.getRecoverable())) {
                jdbc.update("UPDATE sys_job_execution SET state='PENDING',dispatched=FALSE,dispatch_version=dispatch_version+1,owner_token=NULL,lease_until=NULL,recovered=TRUE,next_attempt_time=CURRENT_TIMESTAMP,last_error='执行节点失联，按原执行键恢复',update_time=CURRENT_TIMESTAMP WHERE id=?", row.getId());
            } else {
                jdbc.update("UPDATE sys_job_execution SET state='FAILED',lease_until=NULL,end_time=CURRENT_TIMESTAMP,last_error='执行节点失联；任务未声明可安全恢复，请核对业务结果',update_time=CURRENT_TIMESTAMP WHERE id=?", row.getId());
            }
            return true;
        });
        return Boolean.TRUE.equals(found);
    }

    private void recordInterruptedAttempt(JobExecutionDO row, String reason) {
        JobDO definition = JSONUtil.toBean(row.getSnapshot(), JobDO.class);
        var invocation = new JobInvocation(row.getJobId(), definition.getJobName(), definition.getJobGroup(), definition.getInvokeTarget());
        var context = new JobRunContext(row.getTenantId(), row.getDefinitionRevision(), row.getExecutionKey(),
                row.getTriggerSource(), row.getScheduledFireTime(), Boolean.TRUE.equals(row.getRecovered()), row.getNodeId(),
                row.getId(), row.getAttempt(), Boolean.TRUE.equals(row.getRecoverable()));
        TenantContextHolder.runWithTenantId(row.getTenantId(), () -> logs.save(JobInvokeLogEvent.from(invocation, context)
                .setStatus(1).setJobMessage(reason).setExceptionInfo(reason)));
    }

    JobExecutionDO lockPending() {
        List<JobExecutionDO> rows = jdbc.query("SELECT * FROM sys_job_execution WHERE scheduler_name=? AND state='PENDING' AND dispatched=FALSE AND next_attempt_time<=CURRENT_TIMESTAMP ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED", ROW, options.getSchedulerName());
        return rows.isEmpty() ? null : rows.get(0);
    }

    void dispatched(Long id) {
        jdbc.update("UPDATE sys_job_execution SET dispatched=TRUE,dispatch_version=dispatch_version+1,lease_until=CURRENT_TIMESTAMP+(?*INTERVAL '1 millisecond'),last_error=NULL,update_time=CURRENT_TIMESTAMP WHERE id=?", options.getExecutionLease().toMillis(), id);
    }

    void dispatchFailed(JobExecutionDO execution, RuntimeException error) {
        int attempt = execution.getDispatchAttempts() + 1;
        jdbc.update("UPDATE sys_job_execution SET dispatch_attempts=?,dispatch_version=dispatch_version+1,state=?,last_error=?,next_attempt_time=CURRENT_TIMESTAMP+(?*INTERVAL '1 second'),update_time=CURRENT_TIMESTAMP WHERE id=? AND state='PENDING' AND dispatched=FALSE AND dispatch_version=?",
                attempt, attempt >= options.getMaxDispatchAttempts() ? "FAILED" : "PENDING", errorMessage(error),
                Math.min(60, 1L << Math.min(attempt, 6)), execution.getId(), execution.getDispatchVersion());
    }

    void skipped(Long id, String reason) { skip(id, reason); }
    JobExecutionReceipt receipt(JobExecutionDO row) { return new JobExecutionReceipt(row.getId(), row.getExecutionKey(), row.getState()); }
    String namespace() { return options.getSchedulerName(); }
    TransactionTemplate transaction() { return transaction; }

    private Long currentTenant() {
        Long tenant = TenantContextHolder.getTenantIdOrNull();
        if (tenant == null || !tenants.isEnabledTenant(tenant)) throw new BusinessException(403, "任务操作需要有效租户上下文");
        return tenant;
    }
    private void requireSupported() {
        if (!supported) throw new BusinessException(503, "可靠任务队列需要 PostgreSQL");
    }
    static String errorMessage(RuntimeException error) {
        return StrUtil.subPre(StrUtil.blankToDefault(error.getMessage(), error.getClass().getSimpleName()), 1800);
    }
    private record Claim(JobExecutionDO execution, String owner, JobRunContext context) {}
}
