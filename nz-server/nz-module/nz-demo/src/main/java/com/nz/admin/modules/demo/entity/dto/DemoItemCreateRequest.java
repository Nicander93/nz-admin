package com.nz.admin.modules.demo.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建示例条目请求。
 */
@Data
public class DemoItemCreateRequest {

    @NotBlank(message = "条目名称不能为空")
    @Size(max = 100, message = "条目名称不能超过 100 个字符")
    private String name;

    @NotBlank(message = "分类不能为空")
    @Size(max = 50, message = "分类不能超过 50 个字符")
    private String category;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    private Integer status;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序不能小于 0")
    @Max(value = 999, message = "排序不能大于 999")
    private Integer sort;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
