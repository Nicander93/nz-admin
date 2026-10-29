package com.nz.admin.framework.quartz.job;

import com.nz.admin.common.job.JobExecuteService;
import com.nz.admin.common.job.JobInvocation;
import com.nz.admin.common.job.JobRunContext;
import com.nz.admin.common.job.JobTenantProvider;
import com.nz.admin.framework.quartz.core.QuartzConstants;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;

/** 恢复触发仍携带原计划时刻和执行键，实际业务由持久协调器裁决。 */
@Slf4j
@RequiredArgsConstructor
public class QuartzJob implements Job {
    protected final JobExecuteService jobExecuteService;
    protected final JobTenantProvider jobTenantProvider;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobDataMap values = context.getMergedJobDataMap();
            Long tenant = longValue(values, QuartzConstants.TENANT_ID, null);
            if (!jobTenantProvider.isEnabledTenant(tenant)) return;
            Long id = longValue(values, QuartzConstants.JOB_ID, null);
            Long scheduled = longValue(values, QuartzConstants.SCHEDULED_FIRE_TIME, null);
            if (scheduled == null && context.isRecovering()) scheduled = longValue(values,
                    Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_SCHEDULED_FIRETIME_IN_MILLISECONDS, null);
            if (scheduled == null) scheduled = context.getScheduledFireTime() == null
                    ? System.currentTimeMillis() : context.getScheduledFireTime().getTime();
            String key = values.getString(QuartzConstants.EXECUTION_KEY);
            if (key == null) key = "scheduled:" + tenant + ":" + id + ":" + scheduled;
            String source = values.getString(QuartzConstants.TRIGGER_SOURCE);
            if (source == null) source = "SCHEDULED";
            var execution = new JobRunContext(tenant, longValue(values, QuartzConstants.DEFINITION_REVISION, 1L), key,
                    source, scheduled, context.isRecovering() || Boolean.parseBoolean(values.getString(QuartzConstants.RECOVERED)), context.getScheduler().getSchedulerInstanceId(),
                    longValue(values, QuartzConstants.EXECUTION_ID, null), null,
                    Boolean.parseBoolean(values.getString(QuartzConstants.RECOVERABLE)));
            var invocation = new JobInvocation(id, values.getString(QuartzConstants.JOB_NAME),
                    values.getString(QuartzConstants.JOB_GROUP), values.getString(QuartzConstants.INVOKE_TARGET), execution);
            TenantContextHolder.runWithTenantId(tenant, () -> jobExecuteService.executeOrThrow(invocation));
        } catch (Exception error) {
            throw new JobExecutionException(error);
        }
    }

    private Long longValue(JobDataMap values, String key, Long fallback) {
        Object value = values.get(key);
        return value == null ? fallback : Long.valueOf(value.toString());
    }
}
