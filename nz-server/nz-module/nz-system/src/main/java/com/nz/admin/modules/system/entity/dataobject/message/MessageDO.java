package com.nz.admin.modules.system.entity.dataobject.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 用户站内消息。 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class MessageDO extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long senderId;
    private String category;
    private String type;
    private String source;
    private String title;
    private String summary;
    private String content;
    private String dataJson;
    private String path;
    private Integer readStatus;
    private LocalDateTime readTime;
}
