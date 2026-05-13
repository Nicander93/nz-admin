package com.nz.admin.framework.quartz.core;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.framework.quartz.job.DisallowConcurrentQuartzJob;
import com.nz.admin.framework.quartz.job.QuartzJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;

/**
 * Quartz 调度管理器。
 */
@Slf4j
@RequiredArgsConstructor
public class QuartzSchedulerManager {

    private final Scheduler scheduler;

    public void schedule(QuartzJobDescriptor descriptor) throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(QuartzConstants.JOB_ID, descriptor.getJobId());
        jobDataMap.put(QuartzConstants.INVOKE_TARGET, descriptor.getInvokeTarget());

        Class<? extends Job> jobClass = (descriptor.getConcurrent() != null && descriptor.getConcurrent() == 1)
                ? DisallowConcurrentQuartzJob.class
                : QuartzJob.class;

        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobKey(descriptor.getJobId()))
                .usingJobData(jobDataMap)
                .storeDurably()
                .requestRecovery()
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(descriptor.getJobId()))
                .withSchedule(buildSchedule(descriptor))
                .build();
        scheduler.scheduleJob(jobDetail, trigger);
        log.info("已注册 Quartz 任务 {} cron={}", descriptor.getJobName(), descriptor.getCronExpression());
    }

    public void update(QuartzJobDescriptor descriptor) throws SchedulerException {
        delete(descriptor.getJobId());
        if (isSchedulable(descriptor)) {
            schedule(descriptor);
        }
    }

    public void delete(Long jobId) throws SchedulerException {
        scheduler.deleteJob(jobKey(jobId));
    }

    public void pause(Long jobId) throws SchedulerException {
        scheduler.pauseJob(jobKey(jobId));
    }

    public void resume(Long jobId) throws SchedulerException {
        scheduler.resumeJob(jobKey(jobId));
    }

    public void runOnce(Long jobId) throws SchedulerException {
        scheduler.triggerJob(jobKey(jobId));
    }

    public boolean isSchedulable(QuartzJobDescriptor descriptor) {
        return descriptor.getStatus() != null
                && descriptor.getStatus() == 0
                && StrUtil.isNotBlank(descriptor.getCronExpression())
                && QuartzCronUtils.isValid(descriptor.getCronExpression());
    }

    private CronScheduleBuilder buildSchedule(QuartzJobDescriptor descriptor) {
        CronScheduleBuilder builder = CronScheduleBuilder.cronSchedule(descriptor.getCronExpression())
                .withMisfireHandlingInstructionDoNothing();
        if (descriptor.getMisfirePolicy() == null) {
            return builder;
        }
        return switch (descriptor.getMisfirePolicy()) {
            case 1 -> CronScheduleBuilder.cronSchedule(descriptor.getCronExpression())
                    .withMisfireHandlingInstructionFireAndProceed();
            case 2, 3 -> CronScheduleBuilder.cronSchedule(descriptor.getCronExpression())
                    .withMisfireHandlingInstructionDoNothing();
            default -> builder;
        };
    }

    private JobKey jobKey(Long jobId) {
        return new JobKey(String.valueOf(jobId));
    }

    private TriggerKey triggerKey(Long jobId) {
        return new TriggerKey(jobId + "_trigger");
    }
}
