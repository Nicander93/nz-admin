package com.nz.admin.modules.system.entity.dto.client;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新客户端请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientUpdateRequest extends ClientCreateRequest {

    @NotNull(message = "客户端 ID 不能为空")
    private Long id;
}
