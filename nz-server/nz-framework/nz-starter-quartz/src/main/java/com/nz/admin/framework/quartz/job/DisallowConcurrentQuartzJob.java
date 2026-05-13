package com.nz.admin.framework.quartz.job;

import com.nz.admin.common.job.JobExecuteService;
import org.quartz.DisallowConcurrentExecution;

/**
 * 禁止同一 JobDetail 并发执行的 Quartz Job（对应 sys_job.concurrent = 1）。
 */
@DisallowConcurrentExecution
public class DisallowConcurrentQuartzJob extends QuartzJob {

    public DisallowConcurrentQuartzJob(JobExecuteService jobExecuteService) {
        super(jobExecuteService);
    }
}
