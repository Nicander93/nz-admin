package com.nz.admin.modules.demo.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.modules.demo.convert.DemoItemConvert;
import com.nz.admin.modules.demo.entity.dataobject.DemoItemDO;
import com.nz.admin.modules.demo.entity.dto.DemoItemCreateRequest;
import com.nz.admin.modules.demo.entity.dto.DemoItemUpdateRequest;
import com.nz.admin.modules.demo.mapper.DemoItemMapper;
import org.springframework.stereotype.Service;

/**
 * 示例条目服务实现。
 */
@Service
public class DemoItemServiceImpl extends ServiceImpl<DemoItemMapper, DemoItemDO> implements DemoItemService {

    @Override
    public Page<DemoItemDO> page(Integer pageNum, Integer pageSize, String name, String category, Integer status) {
        LambdaQueryWrapper<DemoItemDO> wrapper = new LambdaQueryWrapper<DemoItemDO>()
                .like(StrUtil.isNotBlank(name), DemoItemDO::getName, name)
                .eq(StrUtil.isNotBlank(category), DemoItemDO::getCategory, category)
                .eq(status != null, DemoItemDO::getStatus, status)
                .orderByAsc(DemoItemDO::getSort)
                .orderByDesc(DemoItemDO::getId);
        return baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public DemoItemDO getRequired(Long id) {
        DemoItemDO item = baseMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("示例条目不存在");
        }
        return item;
    }

    @Override
    public Long create(DemoItemCreateRequest request) {
        DemoItemDO item = DemoItemConvert.toDO(request);
        baseMapper.insert(item);
        return item.getId();
    }

    @Override
    public void update(DemoItemUpdateRequest request) {
        getRequired(request.getId());
        baseMapper.updateById(DemoItemConvert.toDO(request));
    }

    @Override
    public void delete(Long id) {
        getRequired(id);
        baseMapper.deleteById(id);
    }
}
