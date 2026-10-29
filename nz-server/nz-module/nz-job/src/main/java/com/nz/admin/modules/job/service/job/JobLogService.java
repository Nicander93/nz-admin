package com.nz.admin.modules.job.service.job;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.job.entity.dataobject.job.JobLogDO;

public interface JobLogService {

    default Page<JobLogDO> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status) {
        return listPage(pageNum, pageSize, jobName, jobGroup, status, null, null);
    }
    Page<JobLogDO> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status,
                            Long jobId, String executionKey);

    JobLogDO getById(Long id);

    void removeByIds(java.util.List<Long> ids);

    void save(JobLogDO jobLog);
}
