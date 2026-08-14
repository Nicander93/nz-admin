package com.nz.admin.modules.system.entity.dataobject.sms;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import com.nz.admin.framework.encryption.mybatis.EncryptedStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

/** 短信发送记录。 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_sms_send_log", autoResultMap = true)
public class SmsSendLogDO extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long channelId;
    private Long templateId;
    private String phoneNumber;
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String templateCode;
    private String content;
    private String requestParams;
    private String sendStatus;
    private String providerMessageId;
    private String errorMessage;
    private LocalDateTime sendTime;
}
