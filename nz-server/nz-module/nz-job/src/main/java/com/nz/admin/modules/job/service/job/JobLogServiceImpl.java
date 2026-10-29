package com.nz.admin.modules.job.service.job;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nz.admin.common.core.BusinessException;
import org.springframework.transaction.annotation.Transactional;
import com.nz.admin.modules.job.entity.dataobject.job.JobLogDO;
import com.nz.admin.modules.job.mapper.job.JobLogMapper;
import com.nz.admin.modules.job.service.job.JobLogService;
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
    public Page<JobLogDO> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status, Long jobId, String executionKey) {
        if (pageNum == null || pageNum < 1 || pageSize == null || pageSize < 1 || pageSize > 200) throw new BusinessException("分页参数不正确");
        Page<JobLogDO> page = new Page<>(pageNum, pageSize);
        return jobLogMapper.selectPageByCondition(page, jobName, jobGroup, status, jobId, executionKey);
    }

    @Override
    public JobLogDO getById(Long id) {
        JobLogDO row = jobLogMapper.selectById(id);
        if (row == null) throw new BusinessException("日志不存在或无权访问");
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 200 || ids.stream().anyMatch(id -> id == null || id <= 0))
            throw new BusinessException("请选择有效日志");
        List<Long> unique = ids.stream().distinct().toList();
        if (jobLogMapper.selectCount(new LambdaQueryWrapper<JobLogDO>().in(JobLogDO::getId, unique)) != unique.size())
            throw new BusinessException("部分日志不存在或无权访问");
        if (jobLogMapper.deleteByIds(unique) != unique.size()) throw new BusinessException("日志删除失败");
    }

    @Override
    public void save(JobLogDO jobLog) {
        if (jobLogMapper.insert(jobLog) != 1) throw new BusinessException("任务日志写入失败");
    }
}
