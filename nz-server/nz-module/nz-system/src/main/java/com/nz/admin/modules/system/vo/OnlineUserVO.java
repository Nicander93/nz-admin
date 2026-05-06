package com.nz.admin.modules.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 在线用户展示对象。
 */
@Data
public class OnlineUserVO {

    private String tokenValue;
    private Long userId;
    private String username;
    private String deptName;
    private LocalDateTime loginTime;
    private String loginIp;
}
