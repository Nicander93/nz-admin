package @@PACKAGE@@.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.common.core.BusinessException;
import @@PACKAGE@@.entity.dataobject.@@CLASS@@DO;
import @@PACKAGE@@.entity.dto.@@CLASS@@CreateRequest;
import @@PACKAGE@@.entity.dto.@@CLASS@@UpdateRequest;
import @@PACKAGE@@.entity.query.@@CLASS@@Query;
import @@PACKAGE@@.mapper.@@CLASS@@Mapper;
import org.springframework.stereotype.Service;

/**
 * @@FEATURE_DOC@@服务实现。
 *
 * @author @@AUTHOR@@
 */
@Service
public class @@CLASS@@ServiceImpl extends ServiceImpl<@@CLASS@@Mapper, @@CLASS@@DO> implements @@CLASS@@Service {

    @Override
    public Page<@@CLASS@@DO> page(@@CLASS@@Query query) {
        LambdaQueryWrapper<@@CLASS@@DO> wrapper = new LambdaQueryWrapper<@@CLASS@@DO>()
@@QUERY_CONDITIONS@@
                .orderByDesc(@@CLASS@@DO::get@@PK_GETTER@@);
        return baseMapper.selectPage(query.toPage(), wrapper);
    }

    @Override
    public @@CLASS@@DO getRequired(@@PK_TYPE@@ @@PK_FIELD@@) {
        @@CLASS@@DO entity = baseMapper.selectById(@@PK_FIELD@@);
        if (entity == null) {
            throw new BusinessException("@@FEATURE_JAVA@@不存在");
        }
        return entity;
    }

    @Override
    public @@PK_TYPE@@ create(@@CLASS@@CreateRequest request) {
        @@CLASS@@DO entity = BeanUtil.toBean(request, @@CLASS@@DO.class);
        baseMapper.insert(entity);
        return entity.get@@PK_GETTER@@();
    }

    @Override
    public void update(@@CLASS@@UpdateRequest request) {
        @@CLASS@@DO entity = getRequired(request.get@@PK_GETTER@@());
        BeanUtil.copyProperties(request, entity);
        baseMapper.updateById(entity);
    }

    @Override
    public void delete(@@PK_TYPE@@ @@PK_FIELD@@) {
        getRequired(@@PK_FIELD@@);
        baseMapper.deleteById(@@PK_FIELD@@);
    }
}
