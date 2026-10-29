package com.nz.admin.modules.job.service.job;

import com.nz.admin.common.job.JobExecuteService;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.quartz.core.QuartzJobDescriptor;
import com.nz.admin.framework.quartz.core.QuartzSchedulerManager;
import com.nz.admin.modules.job.entity.dataobject.job.JobDO;
import lombok.RequiredArgsConstructor;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.util.List;

/** 调度失败必须让请求或启动失败，不能把未注册任务报告为成功。 */
@Service
@RequiredArgsConstructor
public class QuartzJobService {
    private final Scheduler scheduler;
    private final QuartzSchedulerManager quartzSchedulerManager;
    private final JobExecuteService jobExecuteService;

    public void removeById(Long id) {
        try { quartzSchedulerManager.delete(id); }
        catch (SchedulerException error) { throw schedulingFailure(error); }
    }

    public void scheduleJobIfActive(JobDO job) {
        try {
            if (job.getStatus() != null && job.getStatus() == 0) jobExecuteService.validateTarget(job.getInvokeTarget());
            quartzSchedulerManager.update(toDescriptor(job));
        } catch (SchedulerException error) { throw schedulingFailure(error); }
    }

    public void scheduleJob(JobDO job) throws SchedulerException {
        jobExecuteService.validateTarget(job.getInvokeTarget());
        quartzSchedulerManager.schedule(toDescriptor(job));
    }

    public void scheduleAllJobs(List<JobDO> jobs) {
        try {
            for (JobDO job : jobs) {
                scheduleJobIfActive(job);
            }
            scheduler.start();
        } catch (SchedulerException error) { throw schedulingFailure(error); }
    }

    private BusinessException schedulingFailure(SchedulerException error) {
        var failure = new BusinessException("Quartz 调度失败，请检查任务配置和调度器状态");
        failure.initCause(error);
        return failure;
    }

    private QuartzJobDescriptor toDescriptor(JobDO job) {
        var descriptor = new QuartzJobDescriptor();
        descriptor.setJobId(job.getId());
        descriptor.setTenantId(job.getTenantId());
        descriptor.setRevision(job.getRevision());
        try { descriptor.setRecoverable(jobExecuteService.supportsRecovery(job.getInvokeTarget())); }
        catch (org.springframework.beans.BeansException | IllegalArgumentException error) {
            if (Integer.valueOf(0).equals(job.getStatus())) throw error;
        }
        descriptor.setJobName(job.getJobName());
        descriptor.setJobGroup(cn.hutool.core.util.StrUtil.blankToDefault(job.getJobGroup(), "DEFAULT"));
        descriptor.setInvokeTarget(job.getInvokeTarget());
        descriptor.setCronExpression(job.getCronExpression());
        descriptor.setStatus(job.getStatus());
        descriptor.setConcurrent(job.getConcurrent());
        descriptor.setMisfirePolicy(job.getMisfirePolicy());
        return descriptor;
    }
}
