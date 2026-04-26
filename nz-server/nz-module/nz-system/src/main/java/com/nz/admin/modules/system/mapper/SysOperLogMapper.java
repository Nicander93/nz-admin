package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysOperLog;
import com.nz.admin.modules.system.query.SysOperLogQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {

    default Page<SysOperLog> selectPageByCondition(Page<SysOperLog> page, SysOperLogQuery query) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getTitle()), SysOperLog::getTitle, query.getTitle())
                .like(StrUtil.isNotBlank(query.getOperName()), SysOperLog::getOperName, query.getOperName())
                .eq(query.getBusinessType() != null, SysOperLog::getBusinessType, query.getBusinessType())
                .eq(query.getStatus() != null, SysOperLog::getStatus, query.getStatus())
                .orderByDesc(SysOperLog::getOperTime);
        return selectPage(page, wrapper);
    }
}
