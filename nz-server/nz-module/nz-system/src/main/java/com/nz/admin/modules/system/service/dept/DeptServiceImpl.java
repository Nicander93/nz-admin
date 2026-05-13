package com.nz.admin.modules.system.service.dept;

import com.nz.admin.modules.system.entity.dataobject.dept.DeptDO;
import com.nz.admin.modules.system.mapper.dept.DeptMapper;
import com.nz.admin.modules.system.service.dept.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 部门这块的服务实现。
 */
@Service
public class DeptServiceImpl implements DeptService {

    @Autowired
    private DeptMapper deptMapper;

    /**
     * 把所有部门按排序字段查出来。
     */
    @Override
    public List<DeptDO> listAll() {
        return deptMapper.selectListOrderBySort();
    }

    /**
     * 按 id 拿部门详情。
     */
    @Override
    public DeptDO getById(Long id) {
        return deptMapper.selectById(id);
    }

    /**
     * 新增一条部门记录。
     */
    @Override
    public void save(DeptDO dept) {
        deptMapper.insert(dept);
    }

    /**
     * 按 id 更新部门。
     */
    @Override
    public void updateById(DeptDO dept) {
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
