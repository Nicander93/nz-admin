package com.nz.admin.modules.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.modules.system.entity.po.SysConfigDO;
import com.nz.admin.modules.system.mapper.SysConfigMapper;
import com.nz.admin.modules.system.service.SysConfigService;
import org.springframework.stereotype.Service;

/**
 * 系统参数服务实现。
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfigDO> implements SysConfigService {

    /**
     * 按条件分页查系统参数。
     */
    @Override
    public Page<SysConfigDO> listPage(Integer pageNum, Integer pageSize, String configName, String configKey, Integer status) {
        Page<SysConfigDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(configName), SysConfigDO::getConfigName, configName)
               .like(StrUtil.isNotBlank(configKey), SysConfigDO::getConfigKey, configKey)
               .eq(status != null, SysConfigDO::getStatus, status)
               .orderByDesc(SysConfigDO::getId);
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public SysConfigDO getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public boolean save(SysConfigDO config) {
        return baseMapper.insert(config) > 0;
    }

    @Override
    public boolean updateById(SysConfigDO config) {
        return baseMapper.updateById(config) > 0;
    }

    @Override
    public boolean removeById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }
}
