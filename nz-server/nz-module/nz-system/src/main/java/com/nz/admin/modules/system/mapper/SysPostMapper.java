package com.nz.admin.modules.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.po.SysPostDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysPostMapper extends BaseMapper<SysPostDO> {

    default List<SysPostDO> selectListOrderBySort() {
        return selectList(new LambdaQueryWrapper<SysPostDO>().orderByAsc(SysPostDO::getSort));
    }
}
