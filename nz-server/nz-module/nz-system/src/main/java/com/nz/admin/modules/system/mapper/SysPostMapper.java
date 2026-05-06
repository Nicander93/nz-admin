package com.nz.admin.modules.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.SysPost;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysPostMapper extends BaseMapper<SysPost> {

    default List<SysPost> selectListOrderBySort() {
        return selectList(new LambdaQueryWrapper<SysPost>().orderByAsc(SysPost::getSort));
    }
}
