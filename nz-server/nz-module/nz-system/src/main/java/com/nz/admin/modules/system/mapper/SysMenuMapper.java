package com.nz.admin.modules.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.po.SysMenuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenuDO> {

    default List<SysMenuDO> selectListOrderBySort() {
        return selectList(new LambdaQueryWrapper<SysMenuDO>().orderByAsc(SysMenuDO::getSort));
    }
}
