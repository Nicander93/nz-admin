package com.nz.admin.modules.system.entity.vo.client;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户端响应对象，不包含任何密钥字段。
 */
@Data
public class ClientVO {

    private Long id;
    private String clientId;
    private String clientName;
    private String loginType;
    private Integer tokenTimeout;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
