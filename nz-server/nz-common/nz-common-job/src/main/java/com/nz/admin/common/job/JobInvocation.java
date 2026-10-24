package com.nz.admin.common.job;

/** 每次执行的可信任务身份；日志必须能关联回具体任务。 */
public record JobInvocation(Long jobId, String jobName, String jobGroup, String invokeTarget) {}
