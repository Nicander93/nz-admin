package com.nz.admin.modules.system.mapper.log;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.log.OperLogDO;
import com.nz.admin.modules.system.entity.query.log.OperLogQuery;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface OperLogMapper extends BaseMapper<OperLogDO> {

    default Page<OperLogDO> selectPageByCondition(Page<OperLogDO> page, OperLogQuery query) {
        LambdaQueryWrapper<OperLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getTitle()), OperLogDO::getTitle, query.getTitle())
                .like(StrUtil.isNotBlank(query.getOperName()), OperLogDO::getOperName, query.getOperName())
                .eq(query.getBusinessType() != null, OperLogDO::getBusinessType, query.getBusinessType())
                .eq(query.getStatus() != null, OperLogDO::getStatus, query.getStatus())
                .orderByDesc(OperLogDO::getOperTime);
        return selectPage(page, wrapper);
    }

    default int deleteBeforeOperTime(LocalDateTime deadline) {
        return delete(new LambdaQueryWrapper<OperLogDO>().lt(OperLogDO::getOperTime, deadline));
    }
}
