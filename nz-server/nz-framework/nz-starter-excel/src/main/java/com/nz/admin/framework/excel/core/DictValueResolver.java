package com.nz.admin.framework.excel.core;

/**
 * Excel 字典值转换扩展点。
 */
@FunctionalInterface
public interface DictValueResolver {

    String resolve(String dictType, Object value);
}
