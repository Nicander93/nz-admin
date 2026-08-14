package com.nz.admin.modules.system.entity.vo.sms;

import lombok.Data;
import java.time.LocalDateTime;

/** 短信渠道响应，不返回明文密钥。 */
@Data
public class SmsChannelVO {
    private Long id;
    private String channelCode;
    private String channelName;
    private String providerCode;
    private String endpoint;
    private String accessKeyId;
    private boolean accessKeySecretConfigured;
    private String signature;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
