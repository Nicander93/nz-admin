package com.nz.admin.modules.system.service;

import com.nz.admin.modules.system.entity.po.SysDictDataDO;

import java.util.List;

public interface SysDictDataService {

    List<SysDictDataDO> listByDictType(String dictType);

    SysDictDataDO getById(Long id);

    void save(SysDictDataDO dictData);

    void updateById(SysDictDataDO dictData);

    void removeById(Long id);
}
