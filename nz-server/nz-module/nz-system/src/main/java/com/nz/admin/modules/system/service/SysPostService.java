package com.nz.admin.modules.system.service;

import com.nz.admin.modules.system.entity.po.SysPostDO;

import java.util.List;

public interface SysPostService {

    List<SysPostDO> listAll();

    SysPostDO getById(Long id);

    void save(SysPostDO post);

    void updateById(SysPostDO post);

    void removeById(Long id);
}
