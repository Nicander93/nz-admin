package com.nz.admin.modules.system.entity.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户返回对象。
 */
@Data
@Accessors(chain = true)
public class SysUserRespVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Long deptId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

