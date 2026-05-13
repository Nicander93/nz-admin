package com.nz.admin.modules.system.service.dict;

import com.nz.admin.modules.system.entity.dataobject.dict.DictDataDO;

import java.util.List;

public interface DictDataService {

    List<DictDataDO> listByDictType(String dictType);

    DictDataDO getById(Long id);

    void save(DictDataDO dictData);

    void updateById(DictDataDO dictData);

    void removeById(Long id);
}
