package com.nz.admin.modules.system.service.dict;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.dict.DictTypeDO;
import com.nz.admin.modules.system.mapper.dict.DictTypeMapper;
import com.nz.admin.modules.system.entity.query.dict.DictTypeQuery;
import com.nz.admin.modules.system.service.dict.DictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 字典类型这块的服务实现。
 */
@Service
public class DictTypeServiceImpl implements DictTypeService {

    @Autowired
    private DictTypeMapper dictTypeMapper;

    /**
     * 按分页条件查字典类型。
     */
    @Override
    public Page<DictTypeDO> listPage(DictTypeQuery query) {
        return dictTypeMapper.selectPageByCondition(query.toPage(), query);
    }

    /**
     * 按 id 拿字典类型详情。
     */
    @Override
    public DictTypeDO getById(Long id) {
        return dictTypeMapper.selectById(id);
    }

    /**
     * 新增一条字典类型。
     */
    @Override
    public void save(DictTypeDO dictType) {
        dictTypeMapper.insert(dictType);
    }

    /**
     * 按 id 更新字典类型。
     */
    @Override
    public void updateById(DictTypeDO dictType) {
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
