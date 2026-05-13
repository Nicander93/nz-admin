package com.nz.admin.common.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 按 invokeTarget（beanName.methodName）反射调用 Spring Bean 方法。
 */
@Component
public class JobExecuteService {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JobInvokeLogSink jobInvokeLogSink;

    /**
     * invokeTarget 格式：beanName.methodName
     */
    public void execute(String invokeTarget) {
        JobInvokeLogEvent event = new JobInvokeLogEvent()
                .setInvokeTarget(invokeTarget)
                .setCreateTime(LocalDateTime.now());

        if (invokeTarget == null || invokeTarget.isBlank()) {
            event.setStatus(1);
            event.setJobMessage("调用目标不能为空");
            jobInvokeLogSink.save(event);
            return;
        }

        String[] parts = invokeTarget.split("\\.");
        if (parts.length < 2) {
            event.setStatus(1);
            event.setJobMessage("调用目标格式错误，应为 beanName.methodName");
            jobInvokeLogSink.save(event);
            return;
        }

        String beanName = parts[0];
        String methodName = parts[1];

        try {
            Object bean = applicationContext.getBean(beanName);
            java.lang.reflect.Method method = bean.getClass().getMethod(methodName);
            method.invoke(bean);

            event.setJobMessage("执行成功");
            event.setStatus(0);
        } catch (Exception e) {
            event.setStatus(1);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            event.setJobMessage("执行失败：" + msg);
            event.setExceptionInfo(subPre(msg, 2000));
        }

        jobInvokeLogSink.save(event);
    }

    private static String subPre(String str, int maxLen) {
        if (str == null) {
            return null;
        }
        return str.length() <= maxLen ? str : str.substring(0, maxLen);
    }
}
