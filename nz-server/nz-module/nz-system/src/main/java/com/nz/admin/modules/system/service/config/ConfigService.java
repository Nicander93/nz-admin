package com.nz.admin.modules.system.service.config;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.config.ConfigDO;

public interface ConfigService {

    String getConfigValue(String configKey);

    Page<ConfigDO> listPage(Integer pageNum, Integer pageSize, String configName, String configKey, Integer status);

    ConfigDO getById(Long id);

    boolean save(ConfigDO config);

    boolean updateById(ConfigDO config);

    boolean removeById(Long id);
}
