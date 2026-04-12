package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysRole;
import com.nz.admin.modules.system.query.SysRoleQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    default Page<SysRole> selectPageByCondition(Page<SysRole> page, SysRoleQuery query) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getName()), SysRole::getName, query.getName())
               .like(StrUtil.isNotBlank(query.getRoleKey()), SysRole::getRoleKey, query.getRoleKey())
               .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
               .orderByAsc(SysRole::getSort);
        return selectPage(page, wrapper);
    }
}
