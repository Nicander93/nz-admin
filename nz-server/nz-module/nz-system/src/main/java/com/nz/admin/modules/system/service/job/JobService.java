package com.nz.admin.modules.system.service.job;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.job.JobDO;

import java.util.List;

public interface JobService {

    Page<JobDO> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status);

    JobDO getById(Long id);

    void save(JobDO job);

    void updateById(JobDO job);

    void removeById(Long id);

    /**
     * 立即执行一次任务。
     */
    void runOnce(Long id);

    /**
     * 暂停任务。
     */
    void pauseJob(Long id);

    /**
     * 恢复任务。
     */
    void resumeJob(Long id);

    /**
     * 应用启动时根据库中任务注册 Quartz（仅 status=0 且 cron 非空）。
     */
    void initializeScheduler(List<JobDO> jobs);
}
