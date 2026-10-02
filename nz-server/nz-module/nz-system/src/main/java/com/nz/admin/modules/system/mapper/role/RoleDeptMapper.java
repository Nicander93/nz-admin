package com.nz.admin.modules.system.mapper.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDeptDO;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RoleDeptMapper extends BaseMapper<RoleDeptDO> {
    default List<RoleDeptDO> selectByRoleId(Long roleId) {
        return selectList(new LambdaQueryWrapper<RoleDeptDO>().eq(RoleDeptDO::getRoleId, roleId));
    }

    default void deleteByRoleId(Long roleId) {
        delete(new LambdaQueryWrapper<RoleDeptDO>().eq(RoleDeptDO::getRoleId, roleId));
    }
}
