package com.nz.admin.modules.system.mapper.dept;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.dataobject.dept.DeptDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DeptMapper extends BaseMapper<DeptDO> {

    default List<DeptDO> selectListOrderBySort() {
        return selectList(new LambdaQueryWrapper<DeptDO>().orderByAsc(DeptDO::getSort));
    }
}
