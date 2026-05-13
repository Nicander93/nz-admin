package com.nz.admin.modules.system.mapper.dict;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.dataobject.dict.DictDataDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DictDataMapper extends BaseMapper<DictDataDO> {

    default List<DictDataDO> selectByDictType(String dictType) {
        return selectList(new LambdaQueryWrapper<DictDataDO>()
                .eq(DictDataDO::getDictType, dictType)
                .orderByAsc(DictDataDO::getSort));
    }
}
