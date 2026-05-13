package com.nz.admin.common.job;

/**
 * 任务调用日志落库抽象，由业务模块实现（如写入 sys_job_log）。
 */
public interface JobInvokeLogSink {

    void save(JobInvokeLogEvent event);
}
