package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysLoginLog;
import com.nz.admin.modules.system.query.SysLoginLogQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {

    default Page<SysLoginLog> selectPageByCondition(Page<SysLoginLog> page, SysLoginLogQuery query) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getUsername()), SysLoginLog::getUsername, query.getUsername())
                .like(StrUtil.isNotBlank(query.getIp()), SysLoginLog::getIp, query.getIp())
                .eq(query.getStatus() != null, SysLoginLog::getStatus, query.getStatus())
                .orderByDesc(SysLoginLog::getLoginTime);
        return selectPage(page, wrapper);
    }
}
