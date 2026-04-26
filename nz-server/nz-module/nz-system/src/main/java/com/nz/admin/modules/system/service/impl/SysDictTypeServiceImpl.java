package com.nz.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysDictType;
import com.nz.admin.modules.system.mapper.SysDictTypeMapper;
import com.nz.admin.modules.system.query.SysDictTypeQuery;
import com.nz.admin.modules.system.service.SysDictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 字典类型这块的服务实现。
 */
@Service
public class SysDictTypeServiceImpl implements SysDictTypeService {

    @Autowired
    private SysDictTypeMapper dictTypeMapper;

    /**
     * 按分页条件查字典类型。
     */
    @Override
    public Page<SysDictType> listPage(SysDictTypeQuery query) {
        return dictTypeMapper.selectPageByCondition(query.toPage(), query);
    }

    /**
     * 按 id 拿字典类型详情。
     */
    @Override
    public SysDictType getById(Long id) {
        return dictTypeMapper.selectById(id);
    }

    /**
     * 新增一条字典类型。
     */
    @Override
    public void save(SysDictType dictType) {
        dictTypeMapper.insert(dictType);
    }

    /**
     * 按 id 更新字典类型。
     */
    @Override
    public void updateById(SysDictType dictType) {
        dictTypeMapper.updateById(dictType);
    }

    /**
     * 按 id 删掉字典类型。
     */
    @Override
    public void removeById(Long id) {
        dictTypeMapper.deleteById(id);
    }
}
