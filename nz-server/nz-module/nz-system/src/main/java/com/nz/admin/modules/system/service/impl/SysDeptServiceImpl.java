package com.nz.admin.modules.system.service.impl;

import com.nz.admin.modules.system.entity.po.SysDeptDO;
import com.nz.admin.modules.system.mapper.SysDeptMapper;
import com.nz.admin.modules.system.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 部门这块的服务实现。
 */
@Service
public class SysDeptServiceImpl implements SysDeptService {

    @Autowired
    private SysDeptMapper deptMapper;

    /**
     * 把所有部门按排序字段查出来。
     */
    @Override
    public List<SysDeptDO> listAll() {
        return deptMapper.selectListOrderBySort();
    }

    /**
     * 按 id 拿部门详情。
     */
    @Override
    public SysDeptDO getById(Long id) {
        return deptMapper.selectById(id);
    }

    /**
     * 新增一条部门记录。
     */
    @Override
    public void save(SysDeptDO dept) {
        deptMapper.insert(dept);
    }

    /**
     * 按 id 更新部门。
     */
    @Override
    public void updateById(SysDeptDO dept) {
        deptMapper.updateById(dept);
    }

    /**
     * 按 id 删掉部门。
     */
    @Override
    public void removeById(Long id) {
        deptMapper.deleteById(id);
    }
}
