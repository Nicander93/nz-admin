package com.nz.admin.modules.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysJobDO;
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
    public Page<SysJobDO> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status) {
        Page<SysJobDO> page = new Page<>(pageNum, pageSize);
        return jobMapper.selectPageByCondition(page, jobName, jobGroup, status);
    }

    @Override
    public SysJobDO getById(Long id) {
        return jobMapper.selectById(id);
    }

    @Override
    public void save(SysJobDO job) {
        jobMapper.insert(job);
    }

    @Override
    public void updateById(SysJobDO job) {
        jobMapper.updateById(job);
    }

    @Override
    public void removeById(Long id) {
        jobMapper.deleteById(id);
    }

    @Override
    public void runOnce(Long id) {
        SysJobDO job = jobMapper.selectById(id);
        if (job != null && StrUtil.isNotBlank(job.getInvokeTarget())) {
            jobExecuteService.execute(job.getInvokeTarget());
        }
    }

    @Override
    public void pauseJob(Long id) {
        SysJobDO job = new SysJobDO();
        job.setId(id);
        job.setStatus(1);
        jobMapper.updateById(job);
    }

    @Override
    public void resumeJob(Long id) {
        SysJobDO job = new SysJobDO();
        job.setId(id);
        job.setStatus(0);
        jobMapper.updateById(job);
    }
}
