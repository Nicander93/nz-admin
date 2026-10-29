package com.nz.admin.modules.job.entity.vo.job;

/** 配置已持久化；调度状态由后台同步结果决定。 */
public record JobMutationResult(Long jobId, Long revision, String scheduleState) {}
