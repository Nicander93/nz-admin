package com.nz.admin.modules.system.service.job;

import com.nz.admin.framework.quartz.core.QuartzJobDescriptor;
import com.nz.admin.framework.quartz.core.QuartzSchedulerManager;
import com.nz.admin.modules.system.entity.dataobject.job.JobDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Quartz 任务调度封装，不负责数据库 CRUD。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuartzJobService {

    private final Scheduler scheduler;
    private final QuartzSchedulerManager quartzSchedulerManager;

    public void removeById(Long id) {
        try {
            quartzSchedulerManager.delete(id);
        } catch (SchedulerException e) {
            log.warn("从 Quartz 删除任务失败 id={}", id, e);
        }
    }

    public void scheduleJobIfActive(JobDO job) {
        try {
            quartzSchedulerManager.update(toDescriptor(job));
        } catch (SchedulerException e) {
            log.error("调度任务失败 id={}", job.getId(), e);
        }
    }

    public void scheduleJob(JobDO job) throws SchedulerException {
        quartzSchedulerManager.schedule(toDescriptor(job));
    }

    public void scheduleAllJobs(List<JobDO> jobs) {
        try {
            scheduler.clear();
            for (JobDO job : jobs) {
                if (quartzSchedulerManager.isSchedulable(toDescriptor(job))) {
                    scheduleJob(job);
                }
            }
            if (!scheduler.isShutdown()) {
                scheduler.start();
            }
            log.info("启动时共注册 {} 条任务记录（仅启用的会进调度器）", jobs.size());
        } catch (SchedulerException e) {
            log.error("批量注册 Quartz 任务失败", e);
        }
    }

    private QuartzJobDescriptor toDescriptor(JobDO job) {
        QuartzJobDescriptor descriptor = new QuartzJobDescriptor();
        descriptor.setJobId(job.getId());
        descriptor.setJobName(job.getJobName());
        descriptor.setJobGroup(job.getJobGroup());
        descriptor.setInvokeTarget(job.getInvokeTarget());
        descriptor.setCronExpression(job.getCronExpression());
        descriptor.setStatus(job.getStatus());
        descriptor.setConcurrent(job.getConcurrent());
        descriptor.setMisfirePolicy(job.getMisfirePolicy());
        return descriptor;
    }
}
