package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysLoginLogDO;
import com.nz.admin.modules.system.entity.query.SysLoginLogQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLogDO> {

    default Page<SysLoginLogDO> selectPageByCondition(Page<SysLoginLogDO> page, SysLoginLogQuery query) {
        LambdaQueryWrapper<SysLoginLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getUsername()), SysLoginLogDO::getUsername, query.getUsername())
                .like(StrUtil.isNotBlank(query.getIp()), SysLoginLogDO::getIp, query.getIp())
                .eq(query.getStatus() != null, SysLoginLogDO::getStatus, query.getStatus())
                .orderByDesc(SysLoginLogDO::getLoginTime);
        return selectPage(page, wrapper);
    }
}
