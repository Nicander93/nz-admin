package com.nz.admin.modules.system.entity.vo.sms;

import lombok.Data;
import java.time.LocalDateTime;

/** 短信发送记录响应。 */
@Data
public class SmsSendLogVO {
    private Long id;
    private Long channelId;
    private Long templateId;
    private String phoneNumberMasked;
    private String templateCode;
    private String content;
    private String requestParams;
    private String sendStatus;
    private String providerMessageId;
    private String errorMessage;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
}
