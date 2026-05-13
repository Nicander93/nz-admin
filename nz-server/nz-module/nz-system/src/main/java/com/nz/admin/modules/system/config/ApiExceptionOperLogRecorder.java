package com.nz.admin.modules.system.config;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.web.exception.ApiExceptionLogRecorder;
import com.nz.admin.modules.system.entity.dataobject.log.OperLogDO;
import com.nz.admin.modules.system.service.log.OperLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 全局异常时写入操作日志表，便于排查接口错误。
 */
@Component
public class ApiExceptionOperLogRecorder implements ApiExceptionLogRecorder {

    @Autowired
    private OperLogService operLogService;
    @Autowired
    private LoginUserContext loginUserContext;

    @Override
    public void record(Exception exception, HttpServletRequest request) {
        OperLogDO row = new OperLogDO();
        row.setTitle("接口异常");
        row.setBusinessType(BusinessType.OTHER.getCode());
        row.setOperContent("全局异常-" + exception.getClass().getSimpleName());
        row.setMethod(exception.getClass().getName());
        row.setRequestMethod(request.getMethod());
        row.setOperUrl(StrUtil.subPre(request.getRequestURI(), 255));
        row.setOperIp(resolveClientIp(request));
        row.setUserAgent(StrUtil.subPre(request.getHeader("User-Agent"), 500));
        row.setOperParam(null);
        row.setJsonResult(null);
        row.setStatus(1);
        row.setErrorMsg(StrUtil.subPre(ExceptionUtil.stacktraceToString(exception), 4000));
        row.setOperTime(LocalDateTime.now());
        row.setOperName(resolveOperatorName());
        operLogService.saveAsync(row);
    }

    private String resolveOperatorName() {
        try {
            return loginUserContext.getUsernameOrDefault("anonymous");
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwarded)) {
            int idx = forwarded.indexOf(',');
            return idx >= 0 ? forwarded.substring(0, idx).trim() : forwarded.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
