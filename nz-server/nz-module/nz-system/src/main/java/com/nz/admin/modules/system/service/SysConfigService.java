package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysConfig;

public interface SysConfigService {

    Page<SysConfig> listPage(Integer pageNum, Integer pageSize, String configName, String configKey, Integer status);

    SysConfig getById(Long id);

    boolean save(SysConfig config);

    boolean updateById(SysConfig config);

    boolean removeById(Long id);
}
