package @@PACKAGE@@.entity.query;

import com.nz.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
@@QUERY_IMPORTS@@

/**
 * @@FEATURE_DOC@@分页查询。
 *
 * @author @@AUTHOR@@
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class @@CLASS@@Query extends PageQuery {

@@QUERY_FIELDS@@
}
