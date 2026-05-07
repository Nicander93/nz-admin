package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysJobDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysJobMapper extends BaseMapper<SysJobDO> {

    default Page<SysJobDO> selectPageByCondition(Page<SysJobDO> page, String jobName, String jobGroup, Integer status) {
        LambdaQueryWrapper<SysJobDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(jobName), SysJobDO::getJobName, jobName)
               .like(StrUtil.isNotBlank(jobGroup), SysJobDO::getJobGroup, jobGroup)
               .eq(status != null, SysJobDO::getStatus, status)
               .orderByDesc(SysJobDO::getId);
        return selectPage(page, wrapper);
    }
}
