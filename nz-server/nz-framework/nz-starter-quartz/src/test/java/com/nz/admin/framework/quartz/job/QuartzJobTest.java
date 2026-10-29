package com.nz.admin.framework.quartz.job;

import com.nz.admin.common.job.JobExecuteService;
import com.nz.admin.common.job.JobTenantProvider;
import com.nz.admin.common.job.JobInvocation;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuartzJobTest {

    @Mock
    private JobExecuteService jobExecuteService;

    @Mock
    private JobTenantProvider jobTenantProvider;

    @Mock
    private JobExecutionContext jobExecutionContext;

    private void schedulerIdentity() throws Exception {
        org.quartz.Scheduler scheduler = org.mockito.Mockito.mock(org.quartz.Scheduler.class);
        when(jobExecutionContext.getScheduler()).thenReturn(scheduler);
        when(scheduler.getSchedulerInstanceId()).thenReturn("unit-node");
    }

    @Test
    void shouldSkipExecutionWhenInvokeTargetIsBlank() {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobId", 100L);
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

        QuartzJob quartzJob = new QuartzJob(jobExecuteService, jobTenantProvider);

        assertDoesNotThrow(() -> quartzJob.execute(jobExecutionContext));
        verify(jobExecuteService, never()).executeOrThrow(org.mockito.ArgumentMatchers.any(JobInvocation.class));
    }

    @Test
    void shouldDelegateExecutionToJobExecuteService() throws Exception {
        schedulerIdentity();
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobId", 101L);
        jobDataMap.put("jobName", "unit job");
        jobDataMap.put("jobGroup", "UNIT");
        jobDataMap.put("tenantId", 1L);
        jobDataMap.put("invokeTarget", "demoJob.run");
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

        when(jobTenantProvider.isEnabledTenant(1L)).thenReturn(true);
        QuartzJob quartzJob = new QuartzJob(jobExecuteService, jobTenantProvider);
        quartzJob.execute(jobExecutionContext);

        org.mockito.ArgumentCaptor<JobInvocation> invocation = org.mockito.ArgumentCaptor.forClass(JobInvocation.class);
        verify(jobExecuteService).executeOrThrow(invocation.capture());
        assertEquals(101L, invocation.getValue().jobId());
        assertEquals("unit job", invocation.getValue().jobName());
        assertEquals("UNIT", invocation.getValue().jobGroup());
        assertEquals("demoJob.run", invocation.getValue().invokeTarget());
    }

    @Test
    void shouldRestoreTenantContextForExecution() throws Exception {
        schedulerIdentity();
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobId", 103L);
        jobDataMap.put("jobName", "context job");
        jobDataMap.put("jobGroup", "UNIT");
        jobDataMap.put("tenantId", 7L);
        jobDataMap.put("invokeTarget", "demoJob.run");
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
        when(jobTenantProvider.isEnabledTenant(7L)).thenReturn(true);
        org.mockito.Mockito.doAnswer(invocation -> {
            assertEquals(7L, TenantContextHolder.getTenantIdOrNull());
            return null;
        }).when(jobExecuteService).executeOrThrow(org.mockito.ArgumentMatchers.any(JobInvocation.class));

        QuartzJob quartzJob = new QuartzJob(jobExecuteService, jobTenantProvider);
        quartzJob.execute(jobExecutionContext);

        assertNull(TenantContextHolder.getTenantIdOrNull());
    }

    @Test
    void shouldWrapExecutionFailureAsJobExecutionException() throws Exception {
        schedulerIdentity();
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobId", 102L);
        jobDataMap.put("jobName", "failure job");
        jobDataMap.put("jobGroup", "UNIT");
        jobDataMap.put("tenantId", 1L);
        jobDataMap.put("invokeTarget", "demoJob.run");
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
        when(jobTenantProvider.isEnabledTenant(1L)).thenReturn(true);
        org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
                .when(jobExecuteService)
                .executeOrThrow(org.mockito.ArgumentMatchers.any(JobInvocation.class));

        QuartzJob quartzJob = new QuartzJob(jobExecuteService, jobTenantProvider);

        assertThrows(JobExecutionException.class, () -> quartzJob.execute(jobExecutionContext));
    }

    @Test
    void shouldSkipWhenTenantIsDisabledOrExpired() throws Exception {
        JobDataMap data = new JobDataMap();
        data.put("tenantId", 9L);
        data.put("invokeTarget", "demoJob.run");
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(data);
        when(jobTenantProvider.isEnabledTenant(9L)).thenReturn(false);
        new QuartzJob(jobExecuteService, jobTenantProvider).execute(jobExecutionContext);
        verify(jobExecuteService, never()).executeOrThrow(org.mockito.ArgumentMatchers.any(JobInvocation.class));
        assertNull(TenantContextHolder.getTenantIdOrNull());
    }

    @Test
    void shouldSkipWhenTenantIdIsMissing() throws Exception {
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(new JobDataMap());
        when(jobTenantProvider.isEnabledTenant(null)).thenReturn(false);
        new QuartzJob(jobExecuteService, jobTenantProvider).execute(jobExecutionContext);
        verify(jobExecuteService, never()).executeOrThrow(org.mockito.ArgumentMatchers.any(JobInvocation.class));
    }

    @Test
    void shouldMarkDisallowConcurrentAnnotation() {
        assertNotNull(DisallowConcurrentQuartzJob.class
                .getDeclaredAnnotation(org.quartz.DisallowConcurrentExecution.class));
    }

    @Test void recoveryKeepsOriginalScheduledKeyAndTime() throws Exception {
        schedulerIdentity();
        var data = new JobDataMap();
        data.put("jobId", "12"); data.put("tenantId", "2"); data.put("invokeTarget", "demoTask.run");
        data.put("definitionRevision", "7"); data.put("recoverable", "true");
        data.put(org.quartz.Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_SCHEDULED_FIRETIME_IN_MILLISECONDS, "123456");
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(data);
        when(jobExecutionContext.isRecovering()).thenReturn(true);
        when(jobTenantProvider.isEnabledTenant(2L)).thenReturn(true);
        new QuartzJob(jobExecuteService, jobTenantProvider).execute(jobExecutionContext);
        var captured = org.mockito.ArgumentCaptor.forClass(JobInvocation.class);
        verify(jobExecuteService).executeOrThrow(captured.capture());
        assertEquals("scheduled:2:12:123456", captured.getValue().context().executionKey());
        assertEquals(123456L, captured.getValue().context().scheduledFireTime());
        assertEquals(7L, captured.getValue().context().definitionRevision());
        assertEquals("unit-node", captured.getValue().context().nodeId());
        assertEquals(true, captured.getValue().context().recovered());
    }
}
