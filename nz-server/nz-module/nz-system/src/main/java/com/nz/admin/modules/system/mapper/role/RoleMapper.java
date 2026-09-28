package com.nz.admin.modules.system.mapper.role;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.query.role.RoleQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<RoleDO> {

    default Page<RoleDO> selectPageByCondition(Page<RoleDO> page, RoleQuery query) {
        LambdaQueryWrapper<RoleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getName()), RoleDO::getName, query.getName())
                .like(StrUtil.isNotBlank(query.getRoleKey()), RoleDO::getRoleKey, query.getRoleKey())
                .eq(query.getStatus() != null, RoleDO::getStatus, query.getStatus())
                .eq(query.getDataScope() != null, RoleDO::getDataScope, query.getDataScope())
                .orderByAsc(RoleDO::getSort);
        return selectPage(page, wrapper);
    }

    default List<RoleDO> selectActiveByKeys(Collection<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<RoleDO>()
                .in(RoleDO::getRoleKey, roleKeys)
                .eq(RoleDO::getStatus, 0));
    }
}
