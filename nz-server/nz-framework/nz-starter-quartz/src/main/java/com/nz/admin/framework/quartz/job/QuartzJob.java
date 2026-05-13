package com.nz.admin.framework.quartz.job;

import com.nz.admin.common.job.JobExecuteService;
import com.nz.admin.framework.quartz.core.QuartzConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Quartz 任务实现，委托给 JobExecuteService 执行实际的调用目标。
 */
@Slf4j
@RequiredArgsConstructor
public class QuartzJob implements Job {

    protected final JobExecuteService jobExecuteService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            String invokeTarget = context.getMergedJobDataMap().getString(QuartzConstants.INVOKE_TARGET);
            Long jobId = context.getMergedJobDataMap().getLong(QuartzConstants.JOB_ID);

            log.info("Executing Quartz job {} with invokeTarget: {}", jobId, invokeTarget);

            if (invokeTarget == null || invokeTarget.isBlank()) {
                log.error("Job {} has empty invokeTarget", jobId);
                return;
            }

            jobExecuteService.execute(invokeTarget);
            log.info("Successfully executed Quartz job {}", jobId);
        } catch (Exception e) {
            log.error("Error executing Quartz job", e);
            throw new JobExecutionException(e);
        }
    }
}