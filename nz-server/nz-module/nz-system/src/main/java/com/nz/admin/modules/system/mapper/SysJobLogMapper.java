package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysJobLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLog> {

    default Page<SysJobLog> selectPageByCondition(Page<SysJobLog> page, String jobName, String jobGroup, Integer status) {
        LambdaQueryWrapper<SysJobLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(jobName), SysJobLog::getJobName, jobName)
               .like(StrUtil.isNotBlank(jobGroup), SysJobLog::getJobGroup, jobGroup)
               .eq(status != null, SysJobLog::getStatus, status)
               .orderByDesc(SysJobLog::getCreateTime);
        return selectPage(page, wrapper);
    }
}
