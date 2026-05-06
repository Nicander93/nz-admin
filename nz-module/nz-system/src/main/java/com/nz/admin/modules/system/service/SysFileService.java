package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysFile;
import com.nz.admin.modules.system.query.SysFileQuery;

/**
 * 文件上传服务接口。
 */
public interface SysFileService {

    Page<SysFile> listPage(SysFileQuery query);

    SysFile getById(Long id);

    void save(SysFile file);

    void removeById(Long id);

    void saveBatch(java.util.List<SysFile> files);
}
