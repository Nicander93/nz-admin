package @@PACKAGE@@.entity.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
@@ENTITY_IMPORTS@@

/**
 * @@FEATURE_DOC@@数据对象。
 *
 * @author @@AUTHOR@@
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("@@TABLE@@")
public class @@CLASS@@DO extends BaseEntity {

@@ENTITY_FIELDS@@
}
