package @@PACKAGE@@.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 修改@@FEATURE_DOC@@请求。
 *
 * @author @@AUTHOR@@
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class @@CLASS@@UpdateRequest extends @@CLASS@@CreateRequest {

    @NotNull
    private @@PK_TYPE@@ @@PK_FIELD@@;
}
