package com.nz.admin.common.job;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;

/** 按 beanName.methodName 调用显式声明的任务，并记录每次执行结果。 */
public class JobExecuteService {
    private final ApplicationContext applicationContext;
    private final JobInvokeLogSink jobInvokeLogSink;

    public JobExecuteService(ApplicationContext applicationContext, JobInvokeLogSink jobInvokeLogSink) {
        this.applicationContext = applicationContext;
        this.jobInvokeLogSink = jobInvokeLogSink;
    }

    /** 保存任务之前检查白名单，执行时还会重新检查。 */
    public void validateTarget(String invokeTarget) {
        resolve(invokeTarget);
    }

    /** 执行带有持久化任务身份的任务，让调用方看见执行错误。 */
    public void executeOrThrow(JobInvocation invocation) {
        if (invocation == null || invocation.jobId() == null || invocation.jobId() <= 0) {
            throw new IllegalArgumentException("任务身份必须包含有效任务ID");
        }
        invoke(invocation);
    }

    private void invoke(JobInvocation invocation) {
        String target = invocation.invokeTarget();
        var event = new JobInvokeLogEvent().setJobId(invocation.jobId()).setJobName(invocation.jobName())
                .setJobGroup(invocation.jobGroup()).setInvokeTarget(target).setCreateTime(LocalDateTime.now());
        RuntimeException failure = null;
        try {
            var call = resolve(target);
            ReflectionUtils.invokeMethod(call.method(), call.bean());
            event.setStatus(0);
            event.setJobMessage("执行成功");
        } catch (RuntimeException error) {
            failure = error;
            String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            message = message.substring(0, Math.min(message.length(), 1800));
            event.setStatus(1);
            String summary = "执行失败：" + message;
            event.setJobMessage(summary.substring(0, Math.min(summary.length(), 500)));
            event.setExceptionInfo(message);
        }
        jobInvokeLogSink.save(event);
        if (failure != null) throw new IllegalStateException("任务执行失败，请查看任务日志", failure);
    }

    private Invocation resolve(String target) {
        if (target == null || target.isBlank()) throw new IllegalArgumentException("调用目标不能为空");
        if (!target.matches("[a-zA-Z][a-zA-Z0-9_]*[.][a-zA-Z][a-zA-Z0-9_]*"))
            throw new IllegalArgumentException("调用目标格式错误，应为 beanName.methodName");
        String[] parts = target.split("[.]");
        Object bean = applicationContext.getBean(parts[0]);
        Method method = ClassUtils.getMethodIfAvailable(AopUtils.getTargetClass(bean), parts[1], new Class<?>[0]);
        if (method == null || !Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers())
                || !AnnotatedElementUtils.hasAnnotation(method, JobTask.class))
            throw new IllegalArgumentException("调用目标必须是带 @JobTask 的 public 无参方法");
        Method invocable = AopUtils.selectInvocableMethod(method, bean.getClass());
        ReflectionUtils.makeAccessible(invocable);
        return new Invocation(bean, invocable);
    }

    private record Invocation(Object bean, Method method) {}
}
