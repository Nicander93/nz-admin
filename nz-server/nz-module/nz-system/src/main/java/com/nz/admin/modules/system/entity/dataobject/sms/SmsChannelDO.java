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

/** 短信渠道配置。 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_sms_channel", autoResultMap = true)
public class SmsChannelDO extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String channelCode;
    private String channelName;
    private String providerCode;
    private String endpoint;
    private String accessKeyId;
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String accessKeySecret;
    private String signature;
    private Integer status;
    private String remark;
}
