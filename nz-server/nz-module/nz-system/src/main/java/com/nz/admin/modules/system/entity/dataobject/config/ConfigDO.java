package com.nz.admin.modules.system.entity.dataobject.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 系统参数实体。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class ConfigDO extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String configName;
    private String configKey;
    private String configValue;
    /**
     * 参数类型：1=内置，2=自定义。
     */
    private Integer configType;
    /**
     * 状态：0=正常，1=停用。
     */
    private Integer status;
    private String remark;
}
