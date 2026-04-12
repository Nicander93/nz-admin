package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysDictType;
import com.nz.admin.modules.system.query.SysDictTypeQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictType> {

    default Page<SysDictType> selectPageByCondition(Page<SysDictType> page, SysDictTypeQuery query) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getName()), SysDictType::getName, query.getName())
               .like(StrUtil.isNotBlank(query.getType()), SysDictType::getType, query.getType())
               .eq(query.getStatus() != null, SysDictType::getStatus, query.getStatus());
        return selectPage(page, wrapper);
    }
}
