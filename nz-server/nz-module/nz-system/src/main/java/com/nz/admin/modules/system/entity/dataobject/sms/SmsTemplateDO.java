package com.nz.admin.modules.system.entity.dataobject.sms;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/** 短信模板。 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sms_template")
public class SmsTemplateDO extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long channelId;
    private String templateCode;
    private String templateName;
    private String providerTemplateId;
    private String content;
    private Integer status;
    private String remark;
}
