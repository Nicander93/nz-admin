package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysDictTypeDO;
import com.nz.admin.modules.system.entity.query.SysDictTypeQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictTypeDO> {

    default Page<SysDictTypeDO> selectPageByCondition(Page<SysDictTypeDO> page, SysDictTypeQuery query) {
        LambdaQueryWrapper<SysDictTypeDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getName()), SysDictTypeDO::getName, query.getName())
               .like(StrUtil.isNotBlank(query.getType()), SysDictTypeDO::getType, query.getType())
               .eq(query.getStatus() != null, SysDictTypeDO::getStatus, query.getStatus());
        return selectPage(page, wrapper);
    }
}
