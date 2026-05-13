package com.nz.admin.modules.system.mapper.menu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<MenuDO> {

    default List<MenuDO> selectListOrderBySort() {
        return selectList(new LambdaQueryWrapper<MenuDO>()
                .orderByAsc(MenuDO::getSort)
                .orderByAsc(MenuDO::getId));
    }

    default List<MenuDO> selectByParentId(Long parentId) {
        return selectList(new LambdaQueryWrapper<MenuDO>().eq(MenuDO::getParentId, parentId));
    }
}
