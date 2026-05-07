package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysOperLogDO;
import com.nz.admin.modules.system.entity.query.SysOperLogQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLogDO> {

    default Page<SysOperLogDO> selectPageByCondition(Page<SysOperLogDO> page, SysOperLogQuery query) {
        LambdaQueryWrapper<SysOperLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getTitle()), SysOperLogDO::getTitle, query.getTitle())
                .like(StrUtil.isNotBlank(query.getOperName()), SysOperLogDO::getOperName, query.getOperName())
                .eq(query.getBusinessType() != null, SysOperLogDO::getBusinessType, query.getBusinessType())
                .eq(query.getStatus() != null, SysOperLogDO::getStatus, query.getStatus())
                .orderByDesc(SysOperLogDO::getOperTime);
        return selectPage(page, wrapper);
    }
}
