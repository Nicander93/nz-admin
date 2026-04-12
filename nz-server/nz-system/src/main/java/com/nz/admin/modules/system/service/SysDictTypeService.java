package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysDictType;
import com.nz.admin.modules.system.query.SysDictTypeQuery;

public interface SysDictTypeService {

    Page<SysDictType> listPage(SysDictTypeQuery query);

    SysDictType getById(Long id);

    void save(SysDictType dictType);

    void updateById(SysDictType dictType);

    void removeById(Long id);
}
