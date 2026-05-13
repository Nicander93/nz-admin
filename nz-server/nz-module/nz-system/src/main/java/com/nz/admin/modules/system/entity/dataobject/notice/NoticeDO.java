package com.nz.admin.modules.system.entity.dataobject.notice;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 通知公告实体。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class NoticeDO extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    /**
     * 类型：1=通知，2=公告。
     */
    private Integer type;
    /**
     * 状态：0=正常，1=关闭。
     */
    private Integer status;
    private String remark;
}
