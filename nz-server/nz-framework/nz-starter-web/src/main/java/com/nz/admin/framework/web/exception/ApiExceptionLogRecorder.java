package com.nz.admin.framework.web.exception;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 将未在业务代码中捕获的接口异常落库（由业务模块提供实现，如写入操作日志表）。
 */
@FunctionalInterface
public interface ApiExceptionLogRecorder {

    void record(Exception exception, HttpServletRequest request);
}
