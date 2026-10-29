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
    private final JobExecutionCoordinator executionCoordinator;

    public JobExecuteService(ApplicationContext applicationContext, JobInvokeLogSink jobInvokeLogSink) {
        this(applicationContext, jobInvokeLogSink, (invocation, action) -> {
            JobRunContextHolder.runWith(invocation.context(), action);
            return true;
        });
    }

    public JobExecuteService(ApplicationContext applicationContext, JobInvokeLogSink jobInvokeLogSink,
                             JobExecutionCoordinator executionCoordinator) {
        this.applicationContext = applicationContext;
        this.jobInvokeLogSink = jobInvokeLogSink;
        this.executionCoordinator = java.util.Objects.requireNonNull(executionCoordinator, "Job execution coordinator is required");
    }

    /** 保存任务之前检查白名单，执行时还会重新检查。 */
    public void validateTarget(String invokeTarget) {
        resolve(invokeTarget);
    }

    public boolean supportsRecovery(String invokeTarget) {
        return AnnotatedElementUtils.findMergedAnnotation(resolve(invokeTarget).method(), JobTask.class).recoverable();
    }

    /** 执行带有持久化任务身份的任务，让调用方看见执行错误。 */
    public void executeOrThrow(JobInvocation invocation) {
        if (invocation == null || invocation.jobId() == null || invocation.jobId() <= 0) {
            throw new IllegalArgumentException("任务身份必须包含有效任务ID");
        }
        invoke(invocation);
    }

    private void invoke(JobInvocation invocation) {
        var context = new java.util.concurrent.atomic.AtomicReference<>(invocation.context());
        try {
            executionCoordinator.execute(invocation, () -> {
                context.set(JobRunContextHolder.currentOrNull());
                var call = resolve(invocation.invokeTarget());
                ReflectionUtils.invokeMethod(call.method(), call.bean());
                // 成功日志随业务写入和执行账本一起提交，日志失败也不能伪装成功。
                jobInvokeLogSink.save(JobInvokeLogEvent.from(invocation, context.get()).setStatus(0).setJobMessage("执行成功"));
            });
        } catch (RuntimeException failure) {
            String message = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            message = message.substring(0, Math.min(message.length(), 1800));
            String summary = "执行失败：" + message;
            if (!executionCoordinator.recordsFailures()) {
                try {
                    jobInvokeLogSink.save(JobInvokeLogEvent.from(invocation, context.get()).setStatus(1)
                            .setJobMessage(summary.substring(0, Math.min(summary.length(), 500))).setExceptionInfo(message));
                } catch (RuntimeException loggingFailure) { failure.addSuppressed(loggingFailure); }
            }
            throw new IllegalStateException("任务执行失败，请查看任务日志", failure);
        }
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
