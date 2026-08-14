package com.nz.admin.modules.demo.entity.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 示例条目实体。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("demo_item")
public class DemoItemDO extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private Integer status;
    private Integer sort;
    private String remark;
}
