package com.nz.admin.framework.quartz.core;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.framework.quartz.job.DisallowConcurrentQuartzJob;
import com.nz.admin.framework.quartz.job.QuartzJob;
import lombok.RequiredArgsConstructor;
import org.quartz.*;

import java.util.Map;

/** 只管理 NZ_ADMIN 命名空间；重复对账保留未改变触发器的下一执行时间。 */
@RequiredArgsConstructor
public class QuartzSchedulerManager {
    private final Scheduler scheduler;

    public void schedule(QuartzJobDescriptor descriptor) throws SchedulerException { update(descriptor); }

    public void update(QuartzJobDescriptor descriptor) throws SchedulerException {
        JobKey key = jobKey(descriptor.getJobId());
        JobDetail existing = scheduler.getJobDetail(key);
        long revision = descriptor.getRevision() == null ? 1L : descriptor.getRevision();
        String oldRevision = existing == null ? null : existing.getJobDataMap().getString(QuartzConstants.DEFINITION_REVISION);
        if (StrUtil.isNotBlank(oldRevision) && Long.parseLong(oldRevision) > revision) return;
        JobDataMap values = new JobDataMap();
        values.put(QuartzConstants.JOB_ID, String.valueOf(descriptor.getJobId()));
        values.put(QuartzConstants.TENANT_ID, String.valueOf(descriptor.getTenantId()));
        values.put(QuartzConstants.JOB_NAME, descriptor.getJobName());
        values.put(QuartzConstants.JOB_GROUP, descriptor.getJobGroup());
        values.put(QuartzConstants.INVOKE_TARGET, descriptor.getInvokeTarget());
        values.put(QuartzConstants.DEFINITION_REVISION, String.valueOf(revision));
        values.put(QuartzConstants.RECOVERABLE, String.valueOf(descriptor.isRecoverable()));
        Class<? extends Job> type = Integer.valueOf(1).equals(descriptor.getConcurrent())
                ? DisallowConcurrentQuartzJob.class : QuartzJob.class;
        JobDetail desired = JobBuilder.newJob(type).withIdentity(key).usingJobData(values)
                .storeDurably().requestRecovery(descriptor.isRecoverable()).build();
        if (existing == null || !existing.getJobClass().equals(type)
                || !existing.getJobDataMap().equals(values)
                || existing.requestsRecovery() != desired.requestsRecovery()) {
            scheduler.addJob(desired, true);
        }
        TriggerKey triggerKey = triggerKey(descriptor.getJobId());
        if (!isSchedulable(descriptor)) {
            scheduler.unscheduleJob(triggerKey);
            return;
        }
        CronScheduleBuilder schedule = buildSchedule(descriptor);
        CronTrigger desiredTrigger = TriggerBuilder.newTrigger().withIdentity(triggerKey)
                .forJob(key).withSchedule(schedule).build();
        Trigger current = scheduler.getTrigger(triggerKey);
        if (current instanceof CronTrigger cron && cron.getCronExpression().equals(desiredTrigger.getCronExpression())
                && cron.getTimeZone().equals(desiredTrigger.getTimeZone())
                && cron.getMisfireInstruction() == desiredTrigger.getMisfireInstruction()) return;
        if (current == null) scheduler.scheduleJob(desiredTrigger);
        else scheduler.rescheduleJob(triggerKey, desiredTrigger);
    }

    public void delete(Long jobId) throws SchedulerException { scheduler.deleteJob(jobKey(jobId)); }
    public void pause(Long jobId) throws SchedulerException { scheduler.pauseJob(jobKey(jobId)); }
    public void resume(Long jobId) throws SchedulerException { scheduler.resumeJob(jobKey(jobId)); }
    public void runOnce(Long jobId) throws SchedulerException { runOnce(jobId, Map.of()); }
    public void runOnce(Long jobId, Map<String, String> metadata) throws SchedulerException {
        JobDataMap values = new JobDataMap();
        values.putAll(metadata);
        String executionId = metadata.get(QuartzConstants.EXECUTION_ID);
        if (executionId == null) {
            scheduler.triggerJob(jobKey(jobId), values);
            return;
        }
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(executionTriggerKey(Long.valueOf(executionId)))
                .forJob(jobKey(jobId)).usingJobData(values).startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow()).build();
        try { scheduler.scheduleJob(trigger); }
        catch (ObjectAlreadyExistsException duplicate) {
            // 同一排队记录重发仍使用相同持久触发器；业务执行键另有数据库去重。
        }
    }

    public boolean isSchedulable(QuartzJobDescriptor descriptor) {
        return Integer.valueOf(0).equals(descriptor.getStatus())
                && StrUtil.isNotBlank(descriptor.getCronExpression())
                && QuartzCronUtils.isValid(descriptor.getCronExpression());
    }

    private CronScheduleBuilder buildSchedule(QuartzJobDescriptor descriptor) {
        CronScheduleBuilder builder = CronScheduleBuilder.cronSchedule(descriptor.getCronExpression());
        if (descriptor.getMisfirePolicy() == null) return builder.withMisfireHandlingInstructionDoNothing();
        return switch (descriptor.getMisfirePolicy()) {
            case 1 -> builder.withMisfireHandlingInstructionIgnoreMisfires();
            case 2 -> builder.withMisfireHandlingInstructionFireAndProceed();
            default -> builder.withMisfireHandlingInstructionDoNothing();
        };
    }

    public boolean hasExecutionTrigger(Long executionId) throws SchedulerException {
        return scheduler.checkExists(executionTriggerKey(executionId));
    }
    private static TriggerKey executionTriggerKey(Long id) { return new TriggerKey("execution-" + id, QuartzConstants.OWNED_GROUP); }

    public static JobKey jobKey(Long jobId) { return new JobKey(String.valueOf(jobId), QuartzConstants.OWNED_GROUP); }
    public static TriggerKey triggerKey(Long jobId) { return new TriggerKey(jobId + "_trigger", QuartzConstants.OWNED_GROUP); }
}
