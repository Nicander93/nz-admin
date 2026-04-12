package com.nz.admin.modules.system.service.impl;

import com.nz.admin.modules.system.entity.SysDept;
import com.nz.admin.modules.system.mapper.SysDeptMapper;
import com.nz.admin.modules.system.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysDeptServiceImpl implements SysDeptService {

    @Autowired
    private SysDeptMapper deptMapper;

    @Override
    public List<SysDept> listAll() {
        return deptMapper.selectListOrderBySort();
    }

    @Override
    public SysDept getById(Long id) {
        return deptMapper.selectById(id);
    }

    @Override
    public void save(SysDept dept) {
        deptMapper.insert(dept);
    }

    @Override
    public void updateById(SysDept dept) {
        deptMapper.updateById(dept);
    }

    @Override
    public void removeById(Long id) {
        deptMapper.deleteById(id);
    }
}
