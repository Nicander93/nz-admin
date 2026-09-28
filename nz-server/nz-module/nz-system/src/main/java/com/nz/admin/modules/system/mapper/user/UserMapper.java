package com.nz.admin.modules.system.mapper.user;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.query.user.UserQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    default Page<UserDO> selectPageByCondition(Page<UserDO> page, UserQuery query) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getUsername()), UserDO::getUsername, query.getUsername())
               .like(StrUtil.isNotBlank(query.getNickname()), UserDO::getNickname, query.getNickname())
               .eq(query.getStatus() != null, UserDO::getStatus, query.getStatus())
               .apply(StrUtil.isNotBlank(query.getDataScopeSql()), query.getDataScopeSql());
        return selectPage(page, wrapper);
    }

    default UserDO selectByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username));
    }

    default List<UserDO> selectByPhoneHash(String phoneHash) {
        return selectList(new LambdaQueryWrapper<UserDO>().eq(UserDO::getPhoneHash, phoneHash));
    }

    default List<UserDO> selectActiveByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<UserDO>()
                .in(UserDO::getId, userIds)
                .eq(UserDO::getStatus, 0));
    }
}
