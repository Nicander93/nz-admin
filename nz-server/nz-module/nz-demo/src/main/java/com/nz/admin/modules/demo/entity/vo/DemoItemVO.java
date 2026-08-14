package com.nz.admin.modules.demo.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 示例条目响应。
 */
@Data
public class DemoItemVO {

    private Long id;
    private String name;
    private String category;
    private Integer status;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
