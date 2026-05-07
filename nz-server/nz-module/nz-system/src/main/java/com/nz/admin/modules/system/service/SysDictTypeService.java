package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysDictTypeDO;
import com.nz.admin.modules.system.entity.query.SysDictTypeQuery;

public interface SysDictTypeService {

    Page<SysDictTypeDO> listPage(SysDictTypeQuery query);

    SysDictTypeDO getById(Long id);

    void save(SysDictTypeDO dictType);

    void updateById(SysDictTypeDO dictType);

    void removeById(Long id);
}
