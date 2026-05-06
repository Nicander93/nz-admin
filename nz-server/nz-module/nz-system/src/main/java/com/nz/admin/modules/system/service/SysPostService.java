package com.nz.admin.modules.system.service;

import com.nz.admin.modules.system.entity.SysPost;

import java.util.List;

public interface SysPostService {

    List<SysPost> listAll();

    SysPost getById(Long id);

    void save(SysPost post);

    void updateById(SysPost post);

    void removeById(Long id);
}
