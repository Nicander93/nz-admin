package com.nz.admin.modules.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    default List<SysDept> selectListOrderBySort() {
        return selectList(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort));
    }
}
