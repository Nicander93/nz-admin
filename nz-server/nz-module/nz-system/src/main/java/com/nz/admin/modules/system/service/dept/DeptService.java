package com.nz.admin.modules.system.service.dept;

import com.nz.admin.modules.system.entity.dataobject.dept.DeptDO;

import java.util.List;

public interface DeptService {

    List<DeptDO> listAll();

    DeptDO getById(Long id);

    void save(DeptDO dept);

    void updateById(DeptDO dept);

    void removeById(Long id);
}
