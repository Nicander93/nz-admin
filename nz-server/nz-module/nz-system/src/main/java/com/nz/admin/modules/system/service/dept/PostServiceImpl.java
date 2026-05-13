package com.nz.admin.modules.system.service.dept;

import com.nz.admin.modules.system.entity.dataobject.dept.PostDO;
import com.nz.admin.modules.system.mapper.dept.PostMapper;
import com.nz.admin.modules.system.service.dept.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 岗位服务实现。
 */
@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostMapper postMapper;

    /**
     * 查询所有岗位，按排序字段升序。
     */
    @Override
    public List<PostDO> listAll() {
        return postMapper.selectListOrderBySort();
    }

    /**
     * 按 id 获取岗位详情。
     */
    @Override
    public PostDO getById(Long id) {
        return postMapper.selectById(id);
    }

    /**
     * 新增岗位。
     */
    @Override
    public void save(PostDO post) {
        postMapper.insert(post);
    }

    /**
     * 按 id 更新岗位。
     */
    @Override
    public void updateById(PostDO post) {
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
