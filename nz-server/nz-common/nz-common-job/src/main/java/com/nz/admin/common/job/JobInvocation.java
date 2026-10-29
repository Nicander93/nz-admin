package com.nz.admin.common.job;

/** 可信任务定义和可选执行上下文；旧四参调用继续可用。 */
public record JobInvocation(Long jobId, String jobName, String jobGroup, String invokeTarget,
                            JobRunContext context) {
    public JobInvocation(Long jobId, String jobName, String jobGroup, String invokeTarget) {
        this(jobId, jobName, jobGroup, invokeTarget, null);
    }
}
