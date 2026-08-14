package com.nz.admin.modules.generator.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 单次代码生成参数。
 */
@Data
public class GeneratorRequest {

    @NotBlank
    @Pattern(regexp = "[A-Za-z_][A-Za-z0-9_]*")
    private String schemaName = "public";

    @NotBlank
    @Pattern(regexp = "[A-Za-z_][A-Za-z0-9_]*")
    private String tableName;

    @NotBlank
    @Pattern(regexp = "[a-z][A-Za-z0-9]*")
    private String moduleName;

    @NotBlank
    @Pattern(regexp = "[a-z][A-Za-z0-9]*")
    private String businessName;

    @NotBlank
    @Pattern(regexp = "[A-Z][A-Za-z0-9]*")
    private String className;

    @NotBlank
    @Pattern(regexp = "[a-z_][A-Za-z0-9_]*(\\.[a-z_][A-Za-z0-9_]*)+")
    private String packageName;

    @NotBlank
    private String featureName;

    @NotBlank
    private String author = "nz-admin";

    @NotNull
    private Long parentMenuId = 0L;
}
