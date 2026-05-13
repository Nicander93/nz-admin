package com.nz.admin.modules.system.mapper.log;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.log.LoginLogDO;
import com.nz.admin.modules.system.entity.query.log.LoginLogQuery;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLogDO> {

    default Page<LoginLogDO> selectPageByCondition(Page<LoginLogDO> page, LoginLogQuery query) {
        LambdaQueryWrapper<LoginLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getUsername()), LoginLogDO::getUsername, query.getUsername())
                .like(StrUtil.isNotBlank(query.getIp()), LoginLogDO::getIp, query.getIp())
                .eq(query.getStatus() != null, LoginLogDO::getStatus, query.getStatus())
                .orderByDesc(LoginLogDO::getLoginTime);
        return selectPage(page, wrapper);
    }

    default int deleteBeforeLoginTime(LocalDateTime deadline) {
        return delete(new LambdaQueryWrapper<LoginLogDO>().lt(LoginLogDO::getLoginTime, deadline));
    }
}
