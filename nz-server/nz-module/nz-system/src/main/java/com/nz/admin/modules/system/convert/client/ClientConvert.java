package com.nz.admin.modules.system.convert.client;

import com.nz.admin.modules.system.entity.dataobject.client.ClientDO;
import com.nz.admin.modules.system.entity.dto.client.ClientCreateRequest;
import com.nz.admin.modules.system.entity.dto.client.ClientUpdateRequest;
import com.nz.admin.modules.system.entity.vo.client.ClientVO;

import java.util.List;

/**
 * 客户端对象转换。
 */
public final class ClientConvert {

    private ClientConvert() {
    }

    public static ClientDO toDO(ClientCreateRequest request) {
        ClientDO client = new ClientDO();
        client.setClientId(request.getClientId());
        client.setClientName(request.getClientName());
        client.setLoginType(request.getLoginType());
        client.setTokenTimeout(request.getTokenTimeout());
        client.setStatus(request.getStatus());
        client.setRemark(request.getRemark());
        return client;
    }

    public static ClientDO toDO(ClientUpdateRequest request) {
        ClientDO client = toDO((ClientCreateRequest) request);
        client.setId(request.getId());
        return client;
    }

    public static ClientVO toVO(ClientDO client) {
        ClientVO result = new ClientVO();
        result.setId(client.getId());
        result.setClientId(client.getClientId());
        result.setClientName(client.getClientName());
        result.setLoginType(client.getLoginType());
        result.setTokenTimeout(client.getTokenTimeout());
        result.setStatus(client.getStatus());
        result.setRemark(client.getRemark());
        result.setCreateTime(client.getCreateTime());
        result.setUpdateTime(client.getUpdateTime());
        return result;
    }

    public static List<ClientVO> toVOList(List<ClientDO> clients) {
        return clients.stream().map(ClientConvert::toVO).toList();
    }
}
