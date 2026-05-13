package com.nz.admin.modules.system.service.job;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.job.JobLogDO;
import com.nz.admin.modules.system.mapper.job.JobLogMapper;
import com.nz.admin.modules.system.service.job.JobLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 定时任务日志服务实现。
 */
@Service
public class JobLogServiceImpl implements JobLogService {

    @Autowired
    private JobLogMapper jobLogMapper;

    @Override
    public Page<JobLogDO> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status) {
        Page<JobLogDO> page = new Page<>(pageNum, pageSize);
        return jobLogMapper.selectPageByCondition(page, jobName, jobGroup, status);
    }

    @Override
    public JobLogDO getById(Long id) {
        return jobLogMapper.selectById(id);
    }

    @Override
    public void removeByIds(List<Long> ids) {
        jobLogMapper.deleteBatchIds(ids);
    }

    @Override
    public void save(JobLogDO jobLog) {
        jobLogMapper.insert(jobLog);
    }
}
