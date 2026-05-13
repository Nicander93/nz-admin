package com.nz.admin.modules.system.config;

import com.nz.admin.framework.log.core.OperationLogEvent;
import com.nz.admin.framework.log.core.OperationLogRecorder;
import com.nz.admin.modules.system.entity.dataobject.log.OperLogDO;
import com.nz.admin.modules.system.service.log.OperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 系统模块操作日志落库实现。
 */
@Component
public class OperationLogRecorderImpl implements OperationLogRecorder {

    @Autowired
    private OperLogService operLogService;

    @Override
    public void record(OperationLogEvent event) {
        OperLogDO operLog = new OperLogDO();
        operLog.setTitle(event.getTitle());
        operLog.setBusinessType(event.getBusinessType());
        operLog.setOperContent(event.getOperContent());
        operLog.setMethod(event.getMethod());
        operLog.setRequestMethod(event.getRequestMethod());
        operLog.setOperName(event.getOperName());
        operLog.setOperUrl(event.getOperUrl());
        operLog.setOperIp(event.getOperIp());
        operLog.setUserAgent(event.getUserAgent());
        operLog.setOperParam(event.getOperParam());
        operLog.setJsonResult(event.getJsonResult());
        operLog.setStatus(event.getStatus());
        operLog.setErrorMsg(event.getErrorMsg());
        operLog.setOperTime(event.getOperTime());
        operLogService.saveAsync(operLog);
    }
}
