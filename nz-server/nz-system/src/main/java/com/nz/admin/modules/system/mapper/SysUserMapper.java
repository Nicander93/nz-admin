package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysUser;
import com.nz.admin.modules.system.query.SysUserQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    default Page<SysUser> selectPageByCondition(Page<SysUser> page, SysUserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getUsername()), SysUser::getUsername, query.getUsername())
               .like(StrUtil.isNotBlank(query.getNickname()), SysUser::getNickname, query.getNickname())
               .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus());
        return selectPage(page, wrapper);
    }

    default SysUser selectByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }
}
