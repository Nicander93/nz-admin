package com.nz.admin.modules.job.service.job;

import com.nz.admin.common.job.JobInvokeLogEvent;
import com.nz.admin.common.job.JobInvokeLogSink;
import com.nz.admin.modules.job.entity.dataobject.job.JobLogDO;
import com.nz.admin.modules.job.service.job.JobLogService;
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
        JobLogDO jobLog = cn.hutool.core.bean.BeanUtil.copyProperties(event, JobLogDO.class);
        if (jobLog.getRecovered() == null) jobLog.setRecovered(false);
        if (jobLog.getAttempt() == null) jobLog.setAttempt(1);
        jobLogService.save(jobLog);
    }
}
