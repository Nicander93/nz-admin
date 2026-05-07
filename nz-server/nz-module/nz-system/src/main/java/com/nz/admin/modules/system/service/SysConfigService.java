package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysConfigDO;

public interface SysConfigService {

    Page<SysConfigDO> listPage(Integer pageNum, Integer pageSize, String configName, String configKey, Integer status);

    SysConfigDO getById(Long id);

    boolean save(SysConfigDO config);

    boolean updateById(SysConfigDO config);

    boolean removeById(Long id);
}
