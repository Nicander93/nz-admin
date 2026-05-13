package com.nz.admin.modules.system.mapper.dept;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.dataobject.dept.PostDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<PostDO> {

    default List<PostDO> selectListOrderBySort() {
        return selectList(new LambdaQueryWrapper<PostDO>().orderByAsc(PostDO::getSort));
    }
}
