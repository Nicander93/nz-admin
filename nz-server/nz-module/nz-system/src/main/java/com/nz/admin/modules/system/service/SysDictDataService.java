package com.nz.admin.modules.system.service;

import com.nz.admin.modules.system.entity.SysDictData;

import java.util.List;

public interface SysDictDataService {

    List<SysDictData> listByDictType(String dictType);

    SysDictData getById(Long id);

    void save(SysDictData dictData);

    void updateById(SysDictData dictData);

    void removeById(Long id);
}
