package com.nz.admin.modules.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysJob;
import com.nz.admin.modules.system.mapper.SysJobMapper;
import com.nz.admin.modules.system.service.SysJobService;
import com.nz.admin.modules.system.job.JobExecuteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 定时任务服务实现。
 */
@Service
public class SysJobServiceImpl implements SysJobService {

    @Autowired
    private SysJobMapper jobMapper;

    @Autowired
    private JobExecuteService jobExecuteService;

    @Override
    public Page<SysJob> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status) {
        Page<SysJob> page = new Page<>(pageNum, pageSize);
        return jobMapper.selectPageByCondition(page, jobName, jobGroup, status);
    }

    @Override
    public SysJob getById(Long id) {
        return jobMapper.selectById(id);
    }

    @Override
    public void save(SysJob job) {
        jobMapper.insert(job);
    }

    @Override
    public void updateById(SysJob job) {
        jobMapper.updateById(job);
    }

    @Override
    public void removeById(Long id) {
        jobMapper.deleteById(id);
    }

    @Override
    public void runOnce(Long id) {
        SysJob job = jobMapper.selectById(id);
        if (job != null && StrUtil.isNotBlank(job.getInvokeTarget())) {
            jobExecuteService.execute(job.getInvokeTarget());
        }
    }

    @Override
    public void pauseJob(Long id) {
        SysJob job = new SysJob();
        job.setId(id);
        job.setStatus(1);
        jobMapper.updateById(job);
    }

    @Override
    public void resumeJob(Long id) {
        SysJob job = new SysJob();
        job.setId(id);
        job.setStatus(0);
        jobMapper.updateById(job);
    }
}
