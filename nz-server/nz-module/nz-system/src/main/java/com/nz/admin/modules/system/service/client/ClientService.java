package com.nz.admin.modules.system.service.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.client.ClientDO;
import com.nz.admin.modules.system.entity.dto.client.ClientCreateRequest;
import com.nz.admin.modules.system.entity.dto.client.ClientUpdateRequest;

/**
 * 客户端管理服务。
 */
public interface ClientService {

    Page<ClientDO> page(Integer pageNum, Integer pageSize, String clientId, String clientName, Integer status);

    ClientDO getRequired(Long id);

    Long create(ClientCreateRequest request);

    void update(ClientUpdateRequest request);

    void delete(Long id);
}
