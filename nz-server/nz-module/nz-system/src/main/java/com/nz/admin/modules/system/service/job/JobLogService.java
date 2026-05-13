package com.nz.admin.modules.system.service.job;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.job.JobLogDO;

public interface JobLogService {

    Page<JobLogDO> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status);

    JobLogDO getById(Long id);

    void removeByIds(java.util.List<Long> ids);

    void save(JobLogDO jobLog);
}
