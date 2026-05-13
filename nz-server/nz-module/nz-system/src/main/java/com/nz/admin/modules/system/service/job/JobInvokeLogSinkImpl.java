package com.nz.admin.modules.system.service.job;

import com.nz.admin.common.job.JobInvokeLogEvent;
import com.nz.admin.common.job.JobInvokeLogSink;
import com.nz.admin.modules.system.entity.dataobject.job.JobLogDO;
import com.nz.admin.modules.system.service.job.JobLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 将任务调用日志写入 sys_job_log。
 */
@Component
public class JobInvokeLogSinkImpl implements JobInvokeLogSink {

    @Autowired
    private JobLogService jobLogService;

    @Override
    public void save(JobInvokeLogEvent event) {
        JobLogDO jobLog = new JobLogDO();
        jobLog.setInvokeTarget(event.getInvokeTarget());
        jobLog.setStatus(event.getStatus());
        jobLog.setJobMessage(event.getJobMessage());
        jobLog.setExceptionInfo(event.getExceptionInfo());
        jobLog.setCreateTime(event.getCreateTime());
        jobLogService.save(jobLog);
    }
}
