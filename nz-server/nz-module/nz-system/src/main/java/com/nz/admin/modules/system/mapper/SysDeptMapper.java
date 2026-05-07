package com.nz.admin.modules.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.po.SysDeptDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDeptDO> {

    default List<SysDeptDO> selectListOrderBySort() {
        return selectList(new LambdaQueryWrapper<SysDeptDO>().orderByAsc(SysDeptDO::getSort));
    }
}
