package com.nz.admin.framework.file;

/** 按文件绑定的独立配置创建存储策略；实现方不要复用可变全局配置。 */
@FunctionalInterface
public interface FileStorageAdapterFactory {
    FileStorageService create(FileStorageProperties properties);
}
