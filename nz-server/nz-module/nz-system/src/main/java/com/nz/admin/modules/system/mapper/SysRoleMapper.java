package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysRoleDO;
import com.nz.admin.modules.system.entity.query.SysRoleQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRoleDO> {

    default Page<SysRoleDO> selectPageByCondition(Page<SysRoleDO> page, SysRoleQuery query) {
        LambdaQueryWrapper<SysRoleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getName()), SysRoleDO::getName, query.getName())
               .like(StrUtil.isNotBlank(query.getRoleKey()), SysRoleDO::getRoleKey, query.getRoleKey())
               .eq(query.getStatus() != null, SysRoleDO::getStatus, query.getStatus())
               .orderByAsc(SysRoleDO::getSort);
        return selectPage(page, wrapper);
    }
}
