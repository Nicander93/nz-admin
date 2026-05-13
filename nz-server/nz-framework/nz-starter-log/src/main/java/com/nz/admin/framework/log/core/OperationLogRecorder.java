package com.nz.admin.framework.log.core;

/**
 * 操作日志记录扩展点。
 */
@FunctionalInterface
public interface OperationLogRecorder {

    void record(OperationLogEvent event);
}
