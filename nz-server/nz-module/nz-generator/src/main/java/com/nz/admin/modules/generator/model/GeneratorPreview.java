package com.nz.admin.modules.generator.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 生成预览，文件映射保持模板定义顺序。
 */
@Data
public class GeneratorPreview {

    private List<GeneratorColumn> columns;
    private Map<String, String> files;
}
