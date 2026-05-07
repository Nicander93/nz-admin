package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysNoticeDO;
import com.nz.admin.modules.system.entity.query.SysNoticeQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysNoticeMapper extends BaseMapper<SysNoticeDO> {

    default Page<SysNoticeDO> selectPageByCondition(Page<SysNoticeDO> page, SysNoticeQuery query) {
        LambdaQueryWrapper<SysNoticeDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getTitle()), SysNoticeDO::getTitle, query.getTitle())
               .eq(query.getType() != null, SysNoticeDO::getType, query.getType())
               .eq(query.getStatus() != null, SysNoticeDO::getStatus, query.getStatus())
               .orderByDesc(SysNoticeDO::getId);
        return selectPage(page, wrapper);
    }
}
