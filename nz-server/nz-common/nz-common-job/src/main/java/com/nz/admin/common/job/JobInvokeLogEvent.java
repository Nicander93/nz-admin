package com.nz.admin.common.job;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 任务调用写日志用的事件数据。
 */
@Data
@Accessors(chain = true)
public class JobInvokeLogEvent {

    private Long jobId;
    private Long executionId;
    private String executionKey;
    private String triggerSource;
    private String nodeId;
    private Boolean recovered;
    private Integer attempt;
    private Long scheduledFireTime;
    private String jobName;
    private String jobGroup;
    private String invokeTarget;
    private Integer status;
    private String jobMessage;
    private String exceptionInfo;
    private LocalDateTime createTime;

    public static JobInvokeLogEvent from(JobInvocation invocation, JobRunContext context) {
        var event = new JobInvokeLogEvent().setJobId(invocation.jobId()).setJobName(invocation.jobName())
                .setJobGroup(invocation.jobGroup()).setInvokeTarget(invocation.invokeTarget()).setCreateTime(LocalDateTime.now());
        if (context != null) event.setExecutionId(context.executionId()).setExecutionKey(context.executionKey())
                .setTriggerSource(context.triggerSource()).setNodeId(context.nodeId()).setRecovered(context.recovered())
                .setAttempt(context.attempt()).setScheduledFireTime(context.scheduledFireTime());
        return event;
    }

}
