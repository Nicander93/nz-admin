package com.nz.admin.modules.system.service;

import com.nz.admin.modules.system.entity.po.SysDeptDO;

import java.util.List;

public interface SysDeptService {

    List<SysDeptDO> listAll();

    SysDeptDO getById(Long id);

    void save(SysDeptDO dept);

    void updateById(SysDeptDO dept);

    void removeById(Long id);
}
