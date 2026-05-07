package com.nz.admin.modules.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.po.SysRoleMenuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenuDO> {

    default List<SysRoleMenuDO> selectByRoleId(Long roleId) {
        return selectList(new LambdaQueryWrapper<SysRoleMenuDO>().eq(SysRoleMenuDO::getRoleId, roleId));
    }

    default void deleteByRoleId(Long roleId) {
        delete(new LambdaQueryWrapper<SysRoleMenuDO>().eq(SysRoleMenuDO::getRoleId, roleId));
    }
}
