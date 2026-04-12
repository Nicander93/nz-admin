package com.nz.admin.modules.system.service.impl;

import com.nz.admin.modules.system.entity.SysDictData;
import com.nz.admin.modules.system.mapper.SysDictDataMapper;
import com.nz.admin.modules.system.service.SysDictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysDictDataServiceImpl implements SysDictDataService {

    @Autowired
    private SysDictDataMapper dictDataMapper;

    @Override
    public List<SysDictData> listByDictType(String dictType) {
        return dictDataMapper.selectByDictType(dictType);
    }

    @Override
    public SysDictData getById(Long id) {
        return dictDataMapper.selectById(id);
    }

    @Override
    public void save(SysDictData dictData) {
        dictDataMapper.insert(dictData);
    }

    @Override
    public void updateById(SysDictData dictData) {
        dictDataMapper.updateById(dictData);
    }

    @Override
    public void removeById(Long id) {
        dictDataMapper.deleteById(id);
    }
}
