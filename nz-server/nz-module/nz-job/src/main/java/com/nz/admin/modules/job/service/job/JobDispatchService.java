package com.nz.admin.modules.job.service.job;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.job.JobExecuteService;
import com.nz.admin.common.job.JobTenantProvider;
import com.nz.admin.framework.quartz.config.JobSchedulerProperties;
import com.nz.admin.framework.quartz.core.QuartzConstants;
import com.nz.admin.framework.quartz.core.QuartzSchedulerManager;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.job.entity.dataobject.job.*;
import com.nz.admin.modules.job.entity.vo.job.JobMutationResult;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** 配置意图随业务事务提交，工作节点通过行锁和版本号安全重试 Quartz 操作。 */
@Slf4j
@Service
public class JobDispatchService {
    private static final BeanPropertyRowMapper<JobScheduleOutboxDO> ROW = BeanPropertyRowMapper.newInstance(JobScheduleOutboxDO.class);
    private static final BeanPropertyRowMapper<JobExecutionDO> EXECUTION_ROW = BeanPropertyRowMapper.newInstance(JobExecutionDO.class);
    private static final BeanPropertyRowMapper<JobDO> JOB_ROW = BeanPropertyRowMapper.newInstance(JobDO.class);
    private final JdbcTemplate jdbc;
    private final QuartzJobService quartz;
    private final QuartzSchedulerManager manager;
    private final Scheduler scheduler;
    private final JobExecutionService executions;
    private final JobExecuteService targets;
    private final JobTenantProvider tenants;
    private final JobSchedulerProperties options;
    private volatile boolean started;
    private long nextDefinitionScan;

    public JobDispatchService(JdbcTemplate jdbc, QuartzJobService quartz, QuartzSchedulerManager manager,
                              Scheduler scheduler, JobExecutionService executions, JobExecuteService targets,
                              JobTenantProvider tenants, JobSchedulerProperties options) {
        this.jdbc = jdbc; this.quartz = quartz; this.manager = manager; this.scheduler = scheduler;
        this.executions = executions; this.targets = targets; this.tenants = tenants; this.options = options;
        if (options.getReconcileInterval().toMillis() < 1000) throw new IllegalArgumentException("对账间隔至少1秒");
    }

    public JobMutationResult enqueue(JobDO desired, boolean deleted) {
        if (!executions.isSupported()) throw new BusinessException(503, "可靠任务同步需要 PostgreSQL");
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("调度意图必须与任务定义在同一事务中提交");
        jdbc.update("INSERT INTO sys_job_schedule_outbox(scheduler_name,tenant_id,job_id,revision,operation,payload) VALUES(?,?,?,?,?,?) ON CONFLICT(scheduler_name,tenant_id,job_id) DO UPDATE SET revision=EXCLUDED.revision,operation=EXCLUDED.operation,payload=EXCLUDED.payload,state='PENDING',attempts=0,dispatch_version=sys_job_schedule_outbox.dispatch_version+1,next_attempt_time=CURRENT_TIMESTAMP,last_error=NULL,update_time=CURRENT_TIMESTAMP WHERE sys_job_schedule_outbox.revision<EXCLUDED.revision",
                options.getSchedulerName(), desired.getTenantId(), desired.getId(), desired.getRevision(), deleted ? "DELETE" : "UPSERT", JSONUtil.toJsonStr(desired));
        return new JobMutationResult(desired.getId(), desired.getRevision(), "PENDING");
    }

    public void initialize(List<JobDO> jobs) {
        if (executions.isSupported()) {
            for (JobDO job : jobs) {
                if (Integer.valueOf(0).equals(job.getStatus())) targets.validateTarget(job.getInvokeTarget());
                executions.transaction().executeWithoutResult(status -> {
                    enqueue(job, false);
                    // RAM 重启也必须立即重建；JDBC 重启的重复对账不会重设相同触发器。
                    jdbc.update("UPDATE sys_job_schedule_outbox SET state='PENDING',attempts=0,dispatch_version=dispatch_version+1,next_attempt_time=CURRENT_TIMESTAMP,update_time=CURRENT_TIMESTAMP WHERE scheduler_name=? AND tenant_id=? AND job_id=? AND revision=?",
                            options.getSchedulerName(), job.getTenantId(), job.getId(), job.getRevision());
                });
            }
        }
        started = true;
        synchronizePending();
        try { scheduler.start(); }
        catch (SchedulerException error) { throw failure(error); }
    }

    @Scheduled(fixedDelayString = "${nz.job.sync-interval:1000}", initialDelayString = "${nz.job.sync-initial-delay:1000}")
    public synchronized void synchronizePending() {
        if (!started || !executions.isSupported()) return;
        try {
            if (System.currentTimeMillis() >= nextDefinitionScan) {
                repairDefinitions();
                nextDefinitionScan = System.currentTimeMillis() + options.getReconcileInterval().toMillis();
            }
            for (int count = 0; count < 20 && synchronizeOne(); count++) { }
            for (int count = 0; count < 20 && executions.recoverOneExpired(); count++) { }
            for (int count = 0; count < 20 && recoverOnePendingDispatch(); count++) { }
            for (int count = 0; count < 20 && dispatchOneExecution(); count++) { }
        } catch (RuntimeException error) {
            log.warn("任务同步暂不可用，将在下次轮询重试", error);
        }
    }

    private void repairDefinitions() {
        // 基础设施对账显式遍历各租户，不能借用任意请求线程的租户身份。
        for (int offset = 0; ; offset += 200) {
            List<JobDO> jobs = jdbc.query("SELECT * FROM sys_job ORDER BY id LIMIT 200 OFFSET ?", JOB_ROW, offset);
            for (JobDO job : jobs) executions.transaction().executeWithoutResult(status -> enqueue(job, false));
            if (jobs.size() < 200) return;
        }
    }

    private boolean synchronizeOne() {
        AtomicReference<JobScheduleOutboxDO> attempted = new AtomicReference<>();
        try {
            Boolean handled = executions.transaction().execute(status -> {
                List<JobScheduleOutboxDO> rows = jdbc.query("SELECT * FROM sys_job_schedule_outbox WHERE scheduler_name=? AND state IN ('PENDING','SYNCED') AND next_attempt_time<=CURRENT_TIMESTAMP ORDER BY CASE state WHEN 'PENDING' THEN 0 ELSE 1 END,id LIMIT 1 FOR UPDATE SKIP LOCKED", ROW, options.getSchedulerName());
                if (rows.isEmpty()) return false;
                JobScheduleOutboxDO row = rows.get(0); attempted.set(row);
                JobDO current = executions.definition(row.getTenantId(), row.getJobId());
                if ("DELETE".equals(row.getOperation()) || current == null) quartz.removeById(row.getJobId());
                else {
                    if (!tenants.isEnabledTenant(row.getTenantId())) current.setStatus(1);
                    quartz.scheduleJobIfActive(current);
                }
                jdbc.update("UPDATE sys_job_schedule_outbox SET state='SYNCED',synced_revision=revision,attempts=0,dispatch_version=dispatch_version+1,last_error=NULL,next_attempt_time=CURRENT_TIMESTAMP+(?*INTERVAL '1 millisecond'),update_time=CURRENT_TIMESTAMP WHERE id=?",
                        options.getReconcileInterval().toMillis(), row.getId());
                return true;
            });
            return Boolean.TRUE.equals(handled);
        } catch (RuntimeException error) {
            JobScheduleOutboxDO row = attempted.get();
            if (row == null) throw error;
            executions.transaction().executeWithoutResult(status -> {
                int attempt = row.getAttempts() + 1;
                jdbc.update("UPDATE sys_job_schedule_outbox SET attempts=?,dispatch_version=dispatch_version+1,state=?,last_error=?,next_attempt_time=CURRENT_TIMESTAMP+(?*INTERVAL '1 second'),update_time=CURRENT_TIMESTAMP WHERE id=? AND revision=? AND dispatch_version=?",
                        attempt, attempt >= options.getMaxDispatchAttempts() ? "ERROR" : "PENDING", JobExecutionService.errorMessage(error),
                        Math.min(60, 1L << Math.min(attempt, 6)), row.getId(), row.getRevision(), row.getDispatchVersion());
            });
            log.warn("任务调度同步失败 job={} revision={}", row.getJobId(), row.getRevision(), error);
            return true;
        }
    }

    private boolean dispatchOneExecution() {
        AtomicReference<JobExecutionDO> attempted = new AtomicReference<>();
        try {
            Boolean handled = executions.transaction().execute(status -> {
                JobExecutionDO row = executions.lockPending();
                if (row == null) return false;
                attempted.set(row);
                JobDO current = executions.definition(row.getTenantId(), row.getJobId());
                if (current == null || !tenants.isEnabledTenant(row.getTenantId())
                        || !Objects.equals(current.getRevision(), row.getDefinitionRevision())
                        || ("SCHEDULED".equals(row.getTriggerSource()) && !Integer.valueOf(0).equals(current.getStatus()))) {
                    executions.skipped(row.getId(), "任务、租户或配置版本已失效");
                    return true;
                }
                List<JobScheduleOutboxDO> locks = jdbc.query("SELECT * FROM sys_job_schedule_outbox WHERE scheduler_name=? AND tenant_id=? AND job_id=? FOR UPDATE SKIP LOCKED",
                        ROW, options.getSchedulerName(), row.getTenantId(), row.getJobId());
                if (locks.isEmpty()) {
                    jdbc.update("UPDATE sys_job_execution SET next_attempt_time=CURRENT_TIMESTAMP+INTERVAL '1 second',dispatch_version=dispatch_version+1 WHERE id=?", row.getId());
                    return true;
                }
                quartz.scheduleJobIfActive(current);
                Map<String, String> metadata = new HashMap<>();
                metadata.put(QuartzConstants.EXECUTION_ID, row.getId().toString());
                metadata.put(QuartzConstants.EXECUTION_KEY, row.getExecutionKey());
                metadata.put(QuartzConstants.TRIGGER_SOURCE, row.getTriggerSource());
                metadata.put(QuartzConstants.SCHEDULED_FIRE_TIME, row.getScheduledFireTime().toString());
                metadata.put(QuartzConstants.DEFINITION_REVISION, row.getDefinitionRevision().toString());
                metadata.put(QuartzConstants.RECOVERED, String.valueOf(row.getRecovered()));
                try { manager.runOnce(row.getJobId(), metadata); }
                catch (SchedulerException error) { throw failure(error); }
                executions.dispatched(row.getId());
                return true;
            });
            return Boolean.TRUE.equals(handled);
        } catch (RuntimeException error) {
            JobExecutionDO row = attempted.get();
            if (row == null) throw error;
            executions.transaction().executeWithoutResult(status -> executions.dispatchFailed(row, error));
            log.warn("手动任务派发失败 execution={}", row.getId(), error);
            return true;
        }
    }

    private boolean recoverOnePendingDispatch() {
        Boolean handled = executions.transaction().execute(status -> {
            List<JobExecutionDO> rows = jdbc.query("SELECT * FROM sys_job_execution WHERE scheduler_name=? AND state='PENDING' AND dispatched=TRUE AND lease_until<CURRENT_TIMESTAMP ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED",
                    EXECUTION_ROW, options.getSchedulerName());
            if (rows.isEmpty()) return false;
            JobExecutionDO row = rows.get(0);
            try {
                boolean queued = manager.hasExecutionTrigger(row.getId());
                jdbc.update("UPDATE sys_job_execution SET dispatched=?,dispatch_version=dispatch_version+1,lease_until=CURRENT_TIMESTAMP+(?*INTERVAL '1 millisecond'),next_attempt_time=CURRENT_TIMESTAMP,update_time=CURRENT_TIMESTAMP WHERE id=?",
                        queued, options.getExecutionLease().toMillis(), row.getId());
                return true;
            } catch (SchedulerException error) { throw failure(error); }
        });
        return Boolean.TRUE.equals(handled);
    }

    public void fillStates(List<JobDO> jobs) {
        if (jobs.isEmpty() || !executions.isSupported()) return;
        Map<Long, List<JobDO>> byTenant = new HashMap<>();
        for (JobDO job : jobs) byTenant.computeIfAbsent(job.getTenantId(), ignored -> new ArrayList<>()).add(job);
        byTenant.forEach((tenant, records) -> {
            String placeholders = String.join(",", Collections.nCopies(records.size(), "?"));
            List<Object> values = new ArrayList<>(List.of(options.getSchedulerName(), tenant));
            records.forEach(job -> values.add(job.getId()));
            List<JobScheduleOutboxDO> states = jdbc.query("SELECT * FROM sys_job_schedule_outbox WHERE scheduler_name=? AND tenant_id=? AND job_id IN (" + placeholders + ")", ROW, values.toArray());
            Map<Long, JobScheduleOutboxDO> index = new HashMap<>();
            states.forEach(row -> index.put(row.getJobId(), row));
            records.forEach(job -> {
                JobScheduleOutboxDO row = index.get(job.getId());
                job.setScheduleState(row == null || !Objects.equals(row.getRevision(), job.getRevision()) ? "PENDING" : row.getState());
                job.setScheduleError(row == null ? null : row.getLastError());
            });
        });
    }

    public Page<JobScheduleOutboxDO> list(int page, int size, Long jobId, String state) {
        Long tenant = currentTenant();
        if (page < 1 || size < 1 || size > 200) throw new BusinessException("分页参数不正确");
        String filter = " FROM sys_job_schedule_outbox WHERE scheduler_name=? AND tenant_id=?";
        List<Object> values = new ArrayList<>(List.of(options.getSchedulerName(), tenant));
        if (jobId != null) { filter += " AND job_id=?"; values.add(jobId); }
        if (state != null && !state.isBlank()) { filter += " AND state=?"; values.add(state); }
        Long count = jdbc.queryForObject("SELECT COUNT(*)" + filter, Long.class, values.toArray());
        values.add(size); values.add((page - 1L) * size);
        return new Page<JobScheduleOutboxDO>(page, size, count == null ? 0 : count)
                .setRecords(jdbc.query("SELECT *" + filter + " ORDER BY id DESC LIMIT ? OFFSET ?", ROW, values.toArray()));
    }

    public JobMutationResult retry(Long id) {
        Long tenant = currentTenant();
        return executions.transaction().execute(status -> {
            List<JobScheduleOutboxDO> rows = jdbc.query("SELECT * FROM sys_job_schedule_outbox WHERE id=? AND scheduler_name=? AND tenant_id=? FOR UPDATE", ROW, id, options.getSchedulerName(), tenant);
            if (rows.isEmpty()) throw new BusinessException("同步记录不存在或无权访问");
            JobScheduleOutboxDO row = rows.get(0);
            if ("ERROR".equals(row.getState())) {
                jdbc.update("UPDATE sys_job_schedule_outbox SET state='PENDING',attempts=0,dispatch_version=dispatch_version+1,next_attempt_time=CURRENT_TIMESTAMP,last_error=NULL,update_time=CURRENT_TIMESTAMP WHERE id=?", id);
                return new JobMutationResult(row.getJobId(), row.getRevision(), "PENDING");
            }
            return new JobMutationResult(row.getJobId(), row.getRevision(), row.getState());
        });
    }

    public Map<String, Object> schedulerState() {
        try {
            var metadata = scheduler.getMetaData();
            return Map.of("persistent", metadata.isJobStoreSupportsPersistence(), "clustered", metadata.isJobStoreClustered(),
                    "schedulerName", metadata.getSchedulerName(), "instanceId", metadata.getSchedulerInstanceId(),
                    "running", scheduler.isStarted() && !scheduler.isInStandbyMode(), "jobStoreClass", metadata.getJobStoreClass().getName());
        } catch (SchedulerException error) { throw failure(error); }
    }

    private Long currentTenant() {
        if (!executions.isSupported()) throw new BusinessException(503, "可靠任务同步需要 PostgreSQL");
        Long tenant = TenantContextHolder.getTenantIdOrNull();
        if (tenant == null || !tenants.isEnabledTenant(tenant)) throw new BusinessException(403, "任务操作需要有效租户上下文");
        return tenant;
    }
    private BusinessException failure(SchedulerException error) {
        var failure = new BusinessException(503, "调度器暂不可用，请查看同步与执行记录");
        failure.initCause(error);
        return failure;
    }
}
