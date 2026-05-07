package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysJobLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLogDO> {

    default Page<SysJobLogDO> selectPageByCondition(Page<SysJobLogDO> page, String jobName, String jobGroup, Integer status) {
        LambdaQueryWrapper<SysJobLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(jobName), SysJobLogDO::getJobName, jobName)
               .like(StrUtil.isNotBlank(jobGroup), SysJobLogDO::getJobGroup, jobGroup)
               .eq(status != null, SysJobLogDO::getStatus, status)
               .orderByDesc(SysJobLogDO::getCreateTime);
        return selectPage(page, wrapper);
    }
}
