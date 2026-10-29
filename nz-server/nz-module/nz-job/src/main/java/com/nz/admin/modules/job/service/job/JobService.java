package com.nz.admin.modules.job.service.job;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.job.entity.dataobject.job.JobDO;
import com.nz.admin.modules.job.entity.vo.job.JobMutationResult;
import com.nz.admin.modules.job.entity.vo.job.JobExecutionReceipt;

import java.util.List;

public interface JobService {

    Page<JobDO> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status);

    JobDO getById(Long id);

    JobMutationResult save(JobDO job);

    JobMutationResult updateById(JobDO job);

    JobMutationResult removeById(Long id);

    /**
     * 立即执行一次任务。
     */
    JobExecutionReceipt runOnce(Long id);

    JobExecutionReceipt runOnce(Long id, String requestKey);

    /**
     * 暂停任务。
     */
    JobMutationResult pauseJob(Long id);

    /**
     * 恢复任务。
     */
    JobMutationResult resumeJob(Long id);

    /**
     * 应用启动时根据库中任务注册 Quartz（仅 status=0 且 cron 非空）。
     */
    void initializeScheduler(List<JobDO> jobs);
}
