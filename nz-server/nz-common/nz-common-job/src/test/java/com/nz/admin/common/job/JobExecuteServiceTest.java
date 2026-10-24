package com.nz.admin.common.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobExecuteServiceTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private JobInvokeLogSink jobInvokeLogSink;

    @InjectMocks
    private JobExecuteService jobExecuteService;

    @Test
    void shouldRejectBlankInvokeTarget() {
        assertThrows(IllegalStateException.class, () -> jobExecuteService.executeOrThrow(invocation(101L, " ")));

        JobInvokeLogEvent event = captureSavedEvent();
        assertEquals(1, event.getStatus());
        assertTrue(event.getJobMessage().startsWith("执行失败：调用目标不能为空"));
        assertNotNull(event.getCreateTime());
    }

    @Test
    void shouldRejectInvalidInvokeTargetFormat() {
        assertThrows(IllegalStateException.class, () -> jobExecuteService.executeOrThrow(invocation(102L, "demoBean")));

        JobInvokeLogEvent event = captureSavedEvent();
        assertEquals(1, event.getStatus());
        assertTrue(event.getJobMessage().startsWith("执行失败：调用目标格式错误，应为 beanName.methodName"));
    }

    @Test
    void shouldInvokeBeanMethodWhenTargetIsValid() {
        DemoJobBean bean = new DemoJobBean();
        when(applicationContext.getBean("demoJobBean")).thenReturn(bean);

        jobExecuteService.executeOrThrow(invocation(103L, "demoJobBean.run"));

        JobInvokeLogEvent event = captureSavedEvent();
        assertEquals("done", bean.result);
        assertEquals(0, event.getStatus());
        assertEquals("执行成功", event.getJobMessage());
        assertEquals("demoJobBean.run", event.getInvokeTarget());
        assertEquals(103L, event.getJobId());
    }

    @Test
    void shouldRecordFailureWhenBeanLookupThrowsException() {
        when(applicationContext.getBean("missingBean")).thenThrow(new IllegalStateException("bean not found"));

        assertThrows(IllegalStateException.class, () -> jobExecuteService.executeOrThrow(invocation(104L, "missingBean.run")));

        JobInvokeLogEvent event = captureSavedEvent();
        assertEquals(1, event.getStatus());
        assertEquals("执行失败：bean not found", event.getJobMessage());
        assertEquals("bean not found", event.getExceptionInfo());
    }

    @Test
    void shouldRejectUnmarkedPublicMethod() {
        DemoJobBean bean = new DemoJobBean();
        when(applicationContext.getBean("demoJobBean")).thenReturn(bean);
        assertThrows(IllegalStateException.class, () -> jobExecuteService.executeOrThrow(invocation(105L, "demoJobBean.unmarked")));
        assertEquals(null, bean.result);
        assertTrue(captureSavedEvent().getJobMessage().startsWith("执行失败：调用目标必须是带 @JobTask"));
    }

    @Test
    void shouldRejectStrictlyMalformedTargets() {
        for (String target : new String[]{"demoJobBean.run()", "demoJobBean.run.extra", "1bean.run", "demo-bean.run"}) {
            assertThrows(IllegalArgumentException.class, () -> jobExecuteService.validateTarget(target));
        }
    }

    @Test
    void executeOrThrowPropagatesFailureAfterLogging() {
        DemoJobBean bean = new DemoJobBean();
        when(applicationContext.getBean("demoJobBean")).thenReturn(bean);
        assertThrows(IllegalStateException.class, () -> jobExecuteService.executeOrThrow(invocation(106L, "demoJobBean.fail")));
        assertTrue(captureSavedEvent().getJobMessage().startsWith("执行失败："));
    }

    @Test
    void longTaskFailureStillFitsTheLogSchema() {
        DemoJobBean bean = new DemoJobBean();
        when(applicationContext.getBean("demoJobBean")).thenReturn(bean);
        assertThrows(IllegalStateException.class,
                () -> jobExecuteService.executeOrThrow(invocation(107L, "demoJobBean.failLong")));
        JobInvokeLogEvent event = captureSavedEvent();
        org.junit.jupiter.api.Assertions.assertTrue(event.getJobMessage().length() <= 500);
        assertEquals(1800, event.getExceptionInfo().length());
        assertEquals(1, event.getStatus());
    }

    @Test
    void shouldRejectMissingJobIdBeforeExecutingOrWritingLog() {
        JobInvocation invocation = new JobInvocation(null, "test job", "DEFAULT", "demoJobBean.run");

        assertThrows(IllegalArgumentException.class, () -> jobExecuteService.executeOrThrow(invocation));

        verify(applicationContext, never()).getBean(org.mockito.ArgumentMatchers.anyString());
        verify(jobInvokeLogSink, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private JobInvocation invocation(Long jobId, String target) {
        return new JobInvocation(jobId, "test job", "DEFAULT", target);
    }

    private JobInvokeLogEvent captureSavedEvent() {
        ArgumentCaptor<JobInvokeLogEvent> captor = ArgumentCaptor.forClass(JobInvokeLogEvent.class);
        verify(jobInvokeLogSink).save(captor.capture());
        return captor.getValue();
    }

    static class DemoJobBean {

        private String result;

        @JobTask
        public void failLong() { throw new IllegalStateException("x".repeat(2000)); }


        @JobTask
        public void run() {
            this.result = "done";
        }

        public void unmarked() { this.result = "bad"; }

        @JobTask
        public void fail() { throw new IllegalStateException("boom"); }
    }
}
