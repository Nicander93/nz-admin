package com.nz.admin.modules.system.service.impl;

import com.nz.admin.modules.system.entity.SysPost;
import com.nz.admin.modules.system.mapper.SysPostMapper;
import com.nz.admin.modules.system.service.SysPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 岗位服务实现。
 */
@Service
public class SysPostServiceImpl implements SysPostService {

    @Autowired
    private SysPostMapper postMapper;

    /**
     * 查询所有岗位，按排序字段升序。
     */
    @Override
    public List<SysPost> listAll() {
        return postMapper.selectListOrderBySort();
    }

    /**
     * 按 id 获取岗位详情。
     */
    @Override
    public SysPost getById(Long id) {
        return postMapper.selectById(id);
    }

    /**
     * 新增岗位。
     */
    @Override
    public void save(SysPost post) {
        postMapper.insert(post);
    }

    /**
     * 按 id 更新岗位。
     */
    @Override
    public void updateById(SysPost post) {
        postMapper.updateById(post);
    }

    /**
     * 按 id 删除岗位。
     */
    @Override
    public void removeById(Long id) {
        postMapper.deleteById(id);
    }
}
