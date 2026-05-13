package com.nz.admin.modules.system.entity.dataobject.dict;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class DictTypeDO extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private Integer status;
    private String remark;
}
