package com.nz.admin.modules.system.service.dict;

import com.nz.admin.modules.system.entity.dataobject.dict.DictDataDO;
import com.nz.admin.modules.system.mapper.dict.DictDataMapper;
import com.nz.admin.modules.system.service.dict.DictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典数据这块的服务实现。
 */
@Service
public class DictDataServiceImpl implements DictDataService {

    @Autowired
    private DictDataMapper dictDataMapper;

    /**
     * 按字典类型把数据列表查出来。
     */
    @Override
    public List<DictDataDO> listByDictType(String dictType) {
        return dictDataMapper.selectByDictType(dictType);
    }

    /**
     * 按 id 拿字典数据详情。
     */
    @Override
    public DictDataDO getById(Long id) {
        return dictDataMapper.selectById(id);
    }

    /**
     * 新增一条字典数据。
     */
    @Override
    public void save(DictDataDO dictData) {
        dictDataMapper.insert(dictData);
    }

    /**
     * 按 id 更新字典数据。
     */
    @Override
    public void updateById(DictDataDO dictData) {
        dictDataMapper.updateById(dictData);
    }

    /**
     * 按 id 删掉字典数据。
     */
    @Override
    public void removeById(Long id) {
        dictDataMapper.deleteById(id);
    }
}
