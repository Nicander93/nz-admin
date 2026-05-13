package com.nz.admin.modules.system.mapper.dict;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.dict.DictTypeDO;
import com.nz.admin.modules.system.entity.query.dict.DictTypeQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DictTypeMapper extends BaseMapper<DictTypeDO> {

    default Page<DictTypeDO> selectPageByCondition(Page<DictTypeDO> page, DictTypeQuery query) {
        LambdaQueryWrapper<DictTypeDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getName()), DictTypeDO::getName, query.getName())
               .like(StrUtil.isNotBlank(query.getType()), DictTypeDO::getType, query.getType())
               .eq(query.getStatus() != null, DictTypeDO::getStatus, query.getStatus());
        return selectPage(page, wrapper);
    }
}
