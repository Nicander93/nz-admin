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
        jobExecuteService.execute(" ");

        JobInvokeLogEvent event = captureSavedEvent();
        assertEquals(1, event.getStatus());
        assertEquals("调用目标不能为空", event.getJobMessage());
        assertNotNull(event.getCreateTime());
    }

    @Test
    void shouldRejectInvalidInvokeTargetFormat() {
        jobExecuteService.execute("demoBean");

        JobInvokeLogEvent event = captureSavedEvent();
        assertEquals(1, event.getStatus());
        assertEquals("调用目标格式错误，应为 beanName.methodName", event.getJobMessage());
    }

    @Test
    void shouldInvokeBeanMethodWhenTargetIsValid() {
        DemoJobBean bean = new DemoJobBean();
        when(applicationContext.getBean("demoJobBean")).thenReturn(bean);

        jobExecuteService.execute("demoJobBean.run");

        JobInvokeLogEvent event = captureSavedEvent();
        assertEquals("done", bean.result);
        assertEquals(0, event.getStatus());
        assertEquals("执行成功", event.getJobMessage());
        assertEquals("demoJobBean.run", event.getInvokeTarget());
    }

    @Test
    void shouldRecordFailureWhenBeanLookupThrowsException() {
        when(applicationContext.getBean("missingBean")).thenThrow(new IllegalStateException("bean not found"));

        jobExecuteService.execute("missingBean.run");

        JobInvokeLogEvent event = captureSavedEvent();
        assertEquals(1, event.getStatus());
        assertEquals("执行失败：bean not found", event.getJobMessage());
        assertEquals("bean not found", event.getExceptionInfo());
    }

    private JobInvokeLogEvent captureSavedEvent() {
        ArgumentCaptor<JobInvokeLogEvent> captor = ArgumentCaptor.forClass(JobInvokeLogEvent.class);
        verify(jobInvokeLogSink).save(captor.capture());
        return captor.getValue();
    }

    static class DemoJobBean {

        private String result;

        public void run() {
            this.result = "done";
        }
    }
}
