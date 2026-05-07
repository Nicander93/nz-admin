package com.nz.admin.modules.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.po.SysUserRoleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRoleDO> {

    default List<SysUserRoleDO> selectByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<SysUserRoleDO>().eq(SysUserRoleDO::getUserId, userId));
    }

    default void deleteByUserId(Long userId) {
        delete(new LambdaQueryWrapper<SysUserRoleDO>().eq(SysUserRoleDO::getUserId, userId));
    }
}
