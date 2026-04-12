package com.nz.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysDictType;
import com.nz.admin.modules.system.mapper.SysDictTypeMapper;
import com.nz.admin.modules.system.query.SysDictTypeQuery;
import com.nz.admin.modules.system.service.SysDictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysDictTypeServiceImpl implements SysDictTypeService {

    @Autowired
    private SysDictTypeMapper dictTypeMapper;

    @Override
    public Page<SysDictType> listPage(SysDictTypeQuery query) {
        return dictTypeMapper.selectPageByCondition(query.toPage(), query);
    }

    @Override
    public SysDictType getById(Long id) {
        return dictTypeMapper.selectById(id);
    }

    @Override
    public void save(SysDictType dictType) {
        dictTypeMapper.insert(dictType);
    }

    @Override
    public void updateById(SysDictType dictType) {
        dictTypeMapper.updateById(dictType);
    }

    @Override
    public void removeById(Long id) {
        dictTypeMapper.deleteById(id);
    }
}
