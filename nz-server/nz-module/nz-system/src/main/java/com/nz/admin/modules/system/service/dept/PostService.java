package com.nz.admin.modules.system.service.dept;

import com.nz.admin.modules.system.entity.dataobject.dept.PostDO;

import java.util.List;

public interface PostService {

    List<PostDO> listAll();

    PostDO getById(Long id);

    void save(PostDO post);

    void updateById(PostDO post);

    void removeById(Long id);
}
