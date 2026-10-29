package com.nz.admin.common.job;

/** 一次逻辑执行的稳定身份；外部服务可使用 executionKey 去重副作用。 */
public record JobRunContext(Long tenantId, Long definitionRevision, String executionKey,
                            String triggerSource, Long scheduledFireTime, boolean recovered,
                            String nodeId, Long executionId, Integer attempt, boolean recoverable) {
    public JobRunContext withAttempt(Long id, int number) {
        return new JobRunContext(tenantId, definitionRevision, executionKey, triggerSource, scheduledFireTime,
                recovered, nodeId, id, number, recoverable);
    }
}
