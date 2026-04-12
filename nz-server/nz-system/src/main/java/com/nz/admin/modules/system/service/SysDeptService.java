package com.nz.admin.modules.system.service;

import com.nz.admin.modules.system.entity.SysDept;

import java.util.List;

public interface SysDeptService {

    List<SysDept> listAll();

    SysDept getById(Long id);

    void save(SysDept dept);

    void updateById(SysDept dept);

    void removeById(Long id);
}
