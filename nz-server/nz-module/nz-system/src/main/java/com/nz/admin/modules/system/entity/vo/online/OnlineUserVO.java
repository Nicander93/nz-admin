package com.nz.admin.modules.system.entity.vo.online;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 在线用户展示对象。
 */
@Data
public class OnlineUserVO {

    private String tokenValue;
    private Long userId;
    private Long tenantId;
    private String tenantCode;
    private String username;
    private String deptName;
    private LocalDateTime loginTime;
    private String loginIp;
    private String userAgent;
    private Long tokenTimeout;
}
