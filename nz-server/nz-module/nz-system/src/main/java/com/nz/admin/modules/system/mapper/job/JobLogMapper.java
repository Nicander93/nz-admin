package com.nz.admin.modules.system.mapper.job;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.job.JobLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobLogMapper extends BaseMapper<JobLogDO> {

    default Page<JobLogDO> selectPageByCondition(Page<JobLogDO> page, String jobName, String jobGroup, Integer status) {
        LambdaQueryWrapper<JobLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(jobName), JobLogDO::getJobName, jobName)
               .like(StrUtil.isNotBlank(jobGroup), JobLogDO::getJobGroup, jobGroup)
               .eq(status != null, JobLogDO::getStatus, status)
               .orderByDesc(JobLogDO::getCreateTime);
        return selectPage(page, wrapper);
    }
}
