package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysJob;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysJobMapper extends BaseMapper<SysJob> {

    default Page<SysJob> selectPageByCondition(Page<SysJob> page, String jobName, String jobGroup, Integer status) {
        LambdaQueryWrapper<SysJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(jobName), SysJob::getJobName, jobName)
               .like(StrUtil.isNotBlank(jobGroup), SysJob::getJobGroup, jobGroup)
               .eq(status != null, SysJob::getStatus, status)
               .orderByDesc(SysJob::getId);
        return selectPage(page, wrapper);
    }
}
