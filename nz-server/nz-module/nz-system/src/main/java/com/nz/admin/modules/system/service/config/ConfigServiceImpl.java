package com.nz.admin.modules.system.service.config;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.modules.system.entity.dataobject.config.ConfigDO;
import com.nz.admin.modules.system.mapper.config.ConfigMapper;
import com.nz.admin.modules.system.service.config.ConfigService;
import org.springframework.stereotype.Service;

/**
 * 系统参数服务实现。
 */
@Service
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, ConfigDO> implements ConfigService {

    @Override
    public String getConfigValue(String configKey) {
        if (StrUtil.isBlank(configKey)) {
            return null;
        }
        ConfigDO row = baseMapper.selectOne(
                new LambdaQueryWrapper<ConfigDO>().eq(ConfigDO::getConfigKey, configKey).last("LIMIT 1"));
        return row != null ? row.getConfigValue() : null;
    }

    /**
     * 按条件分页查系统参数。
     */
    @Override
    public Page<ConfigDO> listPage(Integer pageNum, Integer pageSize, String configName, String configKey, Integer status) {
        Page<ConfigDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(configName), ConfigDO::getConfigName, configName)
               .like(StrUtil.isNotBlank(configKey), ConfigDO::getConfigKey, configKey)
               .eq(status != null, ConfigDO::getStatus, status)
               .orderByDesc(ConfigDO::getId);
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public ConfigDO getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public boolean save(ConfigDO config) {
        return baseMapper.insert(config) > 0;
    }

    @Override
    public boolean updateById(ConfigDO config) {
        return baseMapper.updateById(config) > 0;
    }

    @Override
    public boolean removeById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }
}
