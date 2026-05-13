package com.nz.admin.framework.quartz.job;

import com.nz.admin.common.job.JobExecuteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private JobExecutionContext jobExecutionContext;

    @Test
    void shouldSkipExecutionWhenInvokeTargetIsBlank() {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobId", 100L);
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

        QuartzJob quartzJob = new QuartzJob(jobExecuteService);

        assertDoesNotThrow(() -> quartzJob.execute(jobExecutionContext));
        verify(jobExecuteService, never()).execute(any());
    }

    @Test
    void shouldDelegateExecutionToJobExecuteService() throws Exception {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobId", 101L);
        jobDataMap.put("invokeTarget", "demoJob.run");
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

        QuartzJob quartzJob = new QuartzJob(jobExecuteService);
        quartzJob.execute(jobExecutionContext);

        verify(jobExecuteService).execute("demoJob.run");
    }

    @Test
    void shouldWrapExecutionFailureAsJobExecutionException() {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobId", 102L);
        jobDataMap.put("invokeTarget", "demoJob.run");
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
        org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
                .when(jobExecuteService)
                .execute("demoJob.run");

        QuartzJob quartzJob = new QuartzJob(jobExecuteService);

        assertThrows(JobExecutionException.class, () -> quartzJob.execute(jobExecutionContext));
    }

    @Test
    void shouldMarkDisallowConcurrentAnnotation() {
        assertNotNull(DisallowConcurrentQuartzJob.class
                .getDeclaredAnnotation(org.quartz.DisallowConcurrentExecution.class));
    }
}
