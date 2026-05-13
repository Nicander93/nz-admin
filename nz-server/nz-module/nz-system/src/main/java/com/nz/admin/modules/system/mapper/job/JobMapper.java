package com.nz.admin.modules.system.mapper.job;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.job.JobDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobMapper extends BaseMapper<JobDO> {

    default Page<JobDO> selectPageByCondition(Page<JobDO> page, String jobName, String jobGroup, Integer status) {
        LambdaQueryWrapper<JobDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(jobName), JobDO::getJobName, jobName)
               .like(StrUtil.isNotBlank(jobGroup), JobDO::getJobGroup, jobGroup)
               .eq(status != null, JobDO::getStatus, status)
               .orderByDesc(JobDO::getId);
        return selectPage(page, wrapper);
    }
}
