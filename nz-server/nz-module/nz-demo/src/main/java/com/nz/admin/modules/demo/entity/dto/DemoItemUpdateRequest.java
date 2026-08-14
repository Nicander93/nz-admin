package com.nz.admin.modules.demo.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新示例条目请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DemoItemUpdateRequest extends DemoItemCreateRequest {

    @NotNull(message = "条目 ID 不能为空")
    private Long id;
}
