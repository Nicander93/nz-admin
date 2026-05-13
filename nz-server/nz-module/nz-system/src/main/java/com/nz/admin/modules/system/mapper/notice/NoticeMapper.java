package com.nz.admin.modules.system.mapper.notice;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.notice.NoticeDO;
import com.nz.admin.modules.system.entity.query.notice.NoticeQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoticeMapper extends BaseMapper<NoticeDO> {

    default Page<NoticeDO> selectPageByCondition(Page<NoticeDO> page, NoticeQuery query) {
        LambdaQueryWrapper<NoticeDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getTitle()), NoticeDO::getTitle, query.getTitle())
               .eq(query.getType() != null, NoticeDO::getType, query.getType())
               .eq(query.getStatus() != null, NoticeDO::getStatus, query.getStatus())
               .orderByDesc(NoticeDO::getId);
        return selectPage(page, wrapper);
    }
}
