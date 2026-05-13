package com.nz.admin.framework.quartz.core;

import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class QuartzSchedulerManagerTest {

    @Test
    void shouldValidateCronExpression() {
        assertTrue(QuartzCronUtils.isValid("0 0/5 * * * ?"));
        assertFalse(QuartzCronUtils.isValid("bad cron"));
    }

    @Test
    void shouldDelegatePauseResumeDeleteAndRunOnce() throws Exception {
        Scheduler scheduler = mock(Scheduler.class);
        QuartzSchedulerManager manager = new QuartzSchedulerManager(scheduler);

        manager.pause(1L);
        manager.resume(1L);
        manager.delete(1L);
        manager.runOnce(1L);

        verify(scheduler, times(1)).pauseJob(org.quartz.JobKey.jobKey("1"));
        verify(scheduler, times(1)).resumeJob(org.quartz.JobKey.jobKey("1"));
        verify(scheduler, times(1)).deleteJob(org.quartz.JobKey.jobKey("1"));
        verify(scheduler, times(1)).triggerJob(org.quartz.JobKey.jobKey("1"));
    }
}
