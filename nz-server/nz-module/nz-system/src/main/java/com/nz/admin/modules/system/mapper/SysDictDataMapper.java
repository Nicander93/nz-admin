package com.nz.admin.modules.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.po.SysDictDataDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictDataDO> {

    default List<SysDictDataDO> selectByDictType(String dictType) {
        return selectList(new LambdaQueryWrapper<SysDictDataDO>()
                .eq(SysDictDataDO::getDictType, dictType)
                .orderByAsc(SysDictDataDO::getSort));
    }
}
