package com.nz.admin.modules.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysJobLog;
import com.nz.admin.modules.system.mapper.SysJobLogMapper;
import com.nz.admin.modules.system.service.SysJobLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 定时任务日志服务实现。
 */
@Service
public class SysJobLogServiceImpl implements SysJobLogService {

    @Autowired
    private SysJobLogMapper jobLogMapper;

    @Override
    public Page<SysJobLog> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status) {
        Page<SysJobLog> page = new Page<>(pageNum, pageSize);
        return jobLogMapper.selectPageByCondition(page, jobName, jobGroup, status);
    }

    @Override
    public SysJobLog getById(Long id) {
        return jobLogMapper.selectById(id);
    }

    @Override
    public void removeByIds(List<Long> ids) {
        jobLogMapper.deleteBatchIds(ids);
    }

    @Override
    public void save(SysJobLog jobLog) {
        jobLogMapper.insert(jobLog);
    }
}
