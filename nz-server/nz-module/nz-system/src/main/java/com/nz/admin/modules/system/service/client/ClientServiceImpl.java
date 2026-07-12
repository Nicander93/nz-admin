package com.nz.admin.modules.system.service.client;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.modules.system.convert.client.ClientConvert;
import com.nz.admin.modules.system.entity.dataobject.client.ClientDO;
import com.nz.admin.modules.system.entity.dto.client.ClientCreateRequest;
import com.nz.admin.modules.system.entity.dto.client.ClientUpdateRequest;
import com.nz.admin.modules.system.mapper.client.ClientMapper;
import org.springframework.stereotype.Service;

/**
 * 客户端管理服务实现。
 */
@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, ClientDO> implements ClientService {

    @Override
    public Page<ClientDO> page(Integer pageNum, Integer pageSize, String clientId, String clientName, Integer status) {
        LambdaQueryWrapper<ClientDO> wrapper = new LambdaQueryWrapper<ClientDO>()
                .like(StrUtil.isNotBlank(clientId), ClientDO::getClientId, clientId)
                .like(StrUtil.isNotBlank(clientName), ClientDO::getClientName, clientName)
                .eq(status != null, ClientDO::getStatus, status)
                .orderByDesc(ClientDO::getId);
        return baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public ClientDO getRequired(Long id) {
        ClientDO client = baseMapper.selectById(id);
        if (client == null) {
            throw new BusinessException("客户端不存在");
        }
        return client;
    }

    @Override
    public Long create(ClientCreateRequest request) {
        ensureClientIdUnique(request.getClientId(), null);
        ClientDO client = ClientConvert.toDO(request);
        baseMapper.insert(client);
        return client.getId();
    }

    @Override
    public void update(ClientUpdateRequest request) {
        getRequired(request.getId());
        ensureClientIdUnique(request.getClientId(), request.getId());
        baseMapper.updateById(ClientConvert.toDO(request));
    }

    @Override
    public void delete(Long id) {
        getRequired(id);
        baseMapper.deleteById(id);
    }

    private void ensureClientIdUnique(String clientId, Long excludedId) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<ClientDO>()
                .eq(ClientDO::getClientId, clientId)
                .ne(excludedId != null, ClientDO::getId, excludedId));
        if (count != null && count > 0) {
            throw new BusinessException("客户端标识已存在");
        }
    }
}
