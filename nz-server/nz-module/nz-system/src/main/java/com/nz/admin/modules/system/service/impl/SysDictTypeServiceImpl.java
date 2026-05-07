package com.nz.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysDictTypeDO;
import com.nz.admin.modules.system.mapper.SysDictTypeMapper;
import com.nz.admin.modules.system.entity.query.SysDictTypeQuery;
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
    public Page<SysDictTypeDO> listPage(SysDictTypeQuery query) {
        return dictTypeMapper.selectPageByCondition(query.toPage(), query);
    }

    /**
     * 按 id 拿字典类型详情。
     */
    @Override
    public SysDictTypeDO getById(Long id) {
        return dictTypeMapper.selectById(id);
    }

    /**
     * 新增一条字典类型。
     */
    @Override
    public void save(SysDictTypeDO dictType) {
        dictTypeMapper.insert(dictType);
    }

    /**
     * 按 id 更新字典类型。
     */
    @Override
    public void updateById(SysDictTypeDO dictType) {
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
