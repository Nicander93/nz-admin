package com.nz.admin.modules.system.service.dict;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.dict.DictTypeDO;
import com.nz.admin.modules.system.entity.query.dict.DictTypeQuery;

public interface DictTypeService {

    Page<DictTypeDO> listPage(DictTypeQuery query);

    DictTypeDO getById(Long id);

    void save(DictTypeDO dictType);

    void updateById(DictTypeDO dictType);

    void removeById(Long id);
}
