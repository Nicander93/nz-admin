package com.nz.admin.modules.system.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysUserDO;
import com.nz.admin.modules.system.entity.query.SysUserQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUserDO> {

    default Page<SysUserDO> selectPageByCondition(Page<SysUserDO> page, SysUserQuery query) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getUsername()), SysUserDO::getUsername, query.getUsername())
               .like(StrUtil.isNotBlank(query.getNickname()), SysUserDO::getNickname, query.getNickname())
               .eq(query.getStatus() != null, SysUserDO::getStatus, query.getStatus())
               .apply(StrUtil.isNotBlank(query.getDataScopeSql()), query.getDataScopeSql());
        return selectPage(page, wrapper);
    }

    default SysUserDO selectByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<SysUserDO>().eq(SysUserDO::getUsername, username));
    }
}
