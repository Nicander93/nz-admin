package com.nz.admin.modules.system.mapper.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.dataobject.user.UserPostDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserPostMapper extends BaseMapper<UserPostDO> {

    default List<UserPostDO> selectByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<UserPostDO>().eq(UserPostDO::getUserId, userId));
    }

    default void deleteByUserId(Long userId) {
        delete(new LambdaQueryWrapper<UserPostDO>().eq(UserPostDO::getUserId, userId));
    }
}
