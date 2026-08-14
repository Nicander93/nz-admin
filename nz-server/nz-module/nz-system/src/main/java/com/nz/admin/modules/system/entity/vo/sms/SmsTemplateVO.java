package com.nz.admin.modules.system.entity.vo.sms;

import lombok.Data;
import java.time.LocalDateTime;

/** 短信模板响应。 */
@Data
public class SmsTemplateVO {
    private Long id;
    private Long channelId;
    private String channelName;
    private String templateCode;
    private String templateName;
    private String providerTemplateId;
    private String content;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
