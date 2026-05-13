package com.nz.admin.modules.system.mapper.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.dataobject.role.RoleMenuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenuDO> {

    default List<RoleMenuDO> selectByRoleId(Long roleId) {
        return selectList(new LambdaQueryWrapper<RoleMenuDO>().eq(RoleMenuDO::getRoleId, roleId));
    }

    default void deleteByRoleId(Long roleId) {
        delete(new LambdaQueryWrapper<RoleMenuDO>().eq(RoleMenuDO::getRoleId, roleId));
    }

    default void deleteByMenuIds(Collection<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        delete(new LambdaQueryWrapper<RoleMenuDO>().in(RoleMenuDO::getMenuId, menuIds));
    }
}
