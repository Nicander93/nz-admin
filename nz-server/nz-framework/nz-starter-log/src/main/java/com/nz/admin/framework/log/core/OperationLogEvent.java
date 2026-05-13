package com.nz.admin.framework.log.core;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志事件。
 */
@Data
public class OperationLogEvent {

    private String title;
    private Integer businessType;
    private String operContent;
    private String method;
    private String requestMethod;
    private String operName;
    private String operUrl;
    private String operIp;
    private String userAgent;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private String errorMsg;
    private LocalDateTime operTime;
}
