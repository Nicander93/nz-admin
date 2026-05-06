package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysNotice;
import com.nz.admin.modules.system.query.SysNoticeQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysNoticeMapper extends BaseMapper<SysNotice> {

    default Page<SysNotice> selectPageByCondition(Page<SysNotice> page, SysNoticeQuery query) {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getTitle()), SysNotice::getTitle, query.getTitle())
               .eq(query.getType() != null, SysNotice::getType, query.getType())
               .eq(query.getStatus() != null, SysNotice::getStatus, query.getStatus())
               .orderByDesc(SysNotice::getId);
        return selectPage(page, wrapper);
    }
}
