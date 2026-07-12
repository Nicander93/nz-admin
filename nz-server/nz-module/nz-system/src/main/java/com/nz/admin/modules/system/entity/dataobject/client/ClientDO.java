package com.nz.admin.modules.system.entity.dataobject.client;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 登录客户端配置实体。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_client")
public class ClientDO extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String clientId;
    private String clientName;
    private String loginType;
    private Integer tokenTimeout;
    private Integer status;
    private String remark;
}
