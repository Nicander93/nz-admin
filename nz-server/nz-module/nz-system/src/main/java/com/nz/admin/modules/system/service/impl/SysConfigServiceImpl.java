package com.nz.admin.modules.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.modules.system.entity.SysConfig;
import com.nz.admin.modules.system.mapper.SysConfigMapper;
import com.nz.admin.modules.system.service.SysConfigService;
import org.springframework.stereotype.Service;

/**
 * 系统参数服务实现。
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    /**
     * 按条件分页查系统参数。
     */
    @Override
    public Page<SysConfig> listPage(Integer pageNum, Integer pageSize, String configName, String configKey, Integer status) {
        Page<SysConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(configName), SysConfig::getConfigName, configName)
               .like(StrUtil.isNotBlank(configKey), SysConfig::getConfigKey, configKey)
               .eq(status != null, SysConfig::getStatus, status)
               .orderByDesc(SysConfig::getId);
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public SysConfig getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public boolean save(SysConfig config) {
        return baseMapper.insert(config) > 0;
    }

    @Override
    public boolean updateById(SysConfig config) {
        return baseMapper.updateById(config) > 0;
    }

    @Override
    public boolean removeById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }
}
