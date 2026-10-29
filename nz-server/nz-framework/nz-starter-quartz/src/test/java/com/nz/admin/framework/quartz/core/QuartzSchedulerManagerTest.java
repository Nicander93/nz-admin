package com.nz.admin.framework.quartz.core;

import com.nz.admin.framework.quartz.job.QuartzJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QuartzSchedulerManagerTest {
    private Scheduler scheduler;
    private QuartzSchedulerManager manager() throws Exception {
        if (scheduler == null) {
            Properties properties = new Properties();
            properties.setProperty("org.quartz.scheduler.instanceName", "manager-test-" + UUID.randomUUID());
            properties.setProperty("org.quartz.threadPool.threadCount", "1");
            scheduler = new StdSchedulerFactory(properties).getScheduler();
        }
        return new QuartzSchedulerManager(scheduler);
    }
    @AfterEach void close() throws Exception { if (scheduler != null) scheduler.shutdown(false); }

    @Test void validatesCron() {
        assertThat(QuartzCronUtils.isValid("0 0/5 * * * ?")).isTrue();
        assertThat(QuartzCronUtils.isValid("bad cron")).isFalse();
    }

    @Test void repeatedReconcilePreservesNextFireAndCannotOverwriteNewerRevision() throws Exception {
        var manager = manager();
        var job = descriptor(1, 3);
        manager.update(job);
        var key = QuartzSchedulerManager.triggerKey(1L);
        var next = scheduler.getTrigger(key).getNextFireTime();
        manager.update(job);
        assertThat(scheduler.getTrigger(key).getNextFireTime()).isEqualTo(next);
        job.setRevision(2L);
        job.setCronExpression("0 1 0 1 1 ? 2099");
        manager.update(job);
        manager.update(descriptor(1, 3));
        assertThat(((CronTrigger) scheduler.getTrigger(key)).getCronExpression()).isEqualTo("0 1 0 1 1 ? 2099");
        assertThat(scheduler.getJobDetail(QuartzSchedulerManager.jobKey(1L)).getJobDataMap()
                .getString(QuartzConstants.DEFINITION_REVISION)).isEqualTo("2");
    }

    @Test void misfirePoliciesHaveDistinctQuartzInstructions() throws Exception {
        var manager = manager();
        int[] expected = {Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY,
                CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING};
        for (int policy = 1; policy <= 3; policy++) {
            manager.update(descriptor(policy, policy));
            assertThat(scheduler.getTrigger(QuartzSchedulerManager.triggerKey((long) policy)).getMisfireInstruction())
                    .isEqualTo(expected[policy - 1]);
        }
    }

    @Test void pausedDefinitionCanRunManuallyAndStableExecutionTriggerIsNotDuplicated() throws Exception {
        var manager = manager();
        var job = descriptor(7, 3); job.setStatus(1);
        manager.update(job);
        assertThat(scheduler.checkExists(QuartzSchedulerManager.jobKey(7L))).isTrue();
        assertThat(scheduler.getTrigger(QuartzSchedulerManager.triggerKey(7L))).isNull();
        Map<String, String> metadata = Map.of(QuartzConstants.EXECUTION_ID, "55", QuartzConstants.EXECUTION_KEY, "manual:test");
        manager.runOnce(7L, metadata); manager.runOnce(7L, metadata);
        assertThat(scheduler.getTriggersOfJob(QuartzSchedulerManager.jobKey(7L))).hasSize(1);
        assertThat(scheduler.getTriggersOfJob(QuartzSchedulerManager.jobKey(7L)).get(0).getJobDataMap()
                .getString(QuartzConstants.EXECUTION_KEY)).isEqualTo("manual:test");
    }

    @Test void pauseResumeAndDeleteOnlyTouchOwnedNamespace() throws Exception {
        var manager = manager(); manager.schedule(descriptor(1, 3));
        JobKey foreign = new JobKey("1", "FOREIGN");
        scheduler.addJob(JobBuilder.newJob(QuartzJob.class).withIdentity(foreign).storeDurably().build(), false);
        manager.pause(1L);
        assertThat(scheduler.getTriggerState(QuartzSchedulerManager.triggerKey(1L))).isEqualTo(Trigger.TriggerState.PAUSED);
        manager.resume(1L);
        assertThat(scheduler.getTriggerState(QuartzSchedulerManager.triggerKey(1L))).isEqualTo(Trigger.TriggerState.NORMAL);
        manager.delete(1L);
        assertThat(scheduler.checkExists(QuartzSchedulerManager.jobKey(1L))).isFalse();
        assertThat(scheduler.checkExists(foreign)).isTrue();
    }

    private QuartzJobDescriptor descriptor(long id, int policy) {
        var job = new QuartzJobDescriptor();
        job.setJobId(id); job.setTenantId(1L); job.setJobName("unit"); job.setJobGroup("UNIT");
        job.setInvokeTarget("demoTask.run"); job.setCronExpression("0 0 0 1 1 ? 2099");
        job.setStatus(0); job.setConcurrent(1); job.setMisfirePolicy(policy); job.setRevision(1L);
        return job;
    }
}
