package com.nz.admin.framework.log.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.framework.log.core.OperationLogEvent;
import com.nz.admin.framework.log.core.OperationLogRecorder;
import com.nz.admin.framework.log.properties.OperationLogProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志切面。
 */
@Aspect
public class OperationLogAspect {

    private final OperationLogRecorder operationLogRecorder;
    private final LoginUserContext loginUserContext;
    private final OperationLogProperties properties;

    public OperationLogAspect(ObjectProvider<OperationLogRecorder> operationLogRecorderProvider,
                              ObjectProvider<LoginUserContext> loginUserContextProvider,
                              OperationLogProperties properties) {
        this.operationLogRecorder = operationLogRecorderProvider.getIfAvailable();
        this.loginUserContext = loginUserContextProvider.getIfAvailable();
        this.properties = properties;
    }

    @Around("@annotation(log)")
    public Object around(ProceedingJoinPoint joinPoint, Log log) throws Throwable {
        Exception ex = null;
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            ex = e;
            throw e;
        } finally {
            record(joinPoint, log, result, ex);
        }
    }

    private void record(ProceedingJoinPoint joinPoint, Log log, Object result, Exception ex) {
        if (operationLogRecorder == null) {
            return;
        }
        HttpServletRequest request = getRequest();
        if (request == null) {
            return;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        OperationLogEvent event = new OperationLogEvent();
        event.setTitle(log.title());
        event.setBusinessType(log.businessType().getCode());
        event.setOperContent(log.title() + "-" + log.businessType().getDesc());
        event.setMethod(joinPoint.getTarget().getClass().getName() + "." + method.getName() + "()");
        event.setRequestMethod(request.getMethod());
        event.setOperUrl(StrUtil.subPre(request.getRequestURI(), 255));
        event.setOperIp(resolveClientIp(request));
        event.setUserAgent(StrUtil.subPre(request.getHeader("User-Agent"), 500));
        event.setOperName(resolveOperatorName());
        event.setStatus(ex == null ? 0 : 1);
        event.setErrorMsg(ex == null ? null : StrUtil.subPre(ex.getMessage(), 2000));
        event.setOperTime(LocalDateTime.now());
        if (log.recordRequest()) {
            event.setOperParam(buildParam(joinPoint.getArgs()));
        }
        if (log.recordResponse()) {
            event.setJsonResult(buildJsonResult(result));
        }
        operationLogRecorder.record(event);
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private String resolveOperatorName() {
        if (loginUserContext == null) {
            return "anonymous";
        }
        return loginUserContext.getUsernameOrDefault("anonymous");
    }

    private String buildParam(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        List<Object> validArgs = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null || shouldIgnore(arg)) {
                continue;
            }
            validArgs.add(arg);
        }
        return StrUtil.subPre(maskSecrets(JSONUtil.toJsonStr(validArgs)), 2000);
    }

    private String maskSecrets(String json) {
        if (StrUtil.isBlank(json)) {
            return json;
        }
        try {
            JSONArray array = JSONUtil.parseArray(json);
            for (int i = 0; i < array.size(); i++) {
                Object current = array.get(i);
                if (current instanceof JSONObject jsonObject) {
                    for (String sensitiveField : properties.getSensitiveFields()) {
                        jsonObject.remove(sensitiveField);
                    }
                }
            }
            return array.toString();
        } catch (Exception ignored) {
            return json;
        }
    }

    private String buildJsonResult(Object result) {
        if (result == null) {
            return null;
        }
        try {
            return StrUtil.subPre(maskSecrets(JSONUtil.toJsonStr(result)), 2000);
        } catch (Exception e) {
            return StrUtil.subPre(String.valueOf(result), 2000);
        }
    }

    private boolean shouldIgnore(Object arg) {
        String className = arg.getClass().getName();
        return "jakarta.servlet.http.HttpServletRequest".equals(className)
                || "jakarta.servlet.http.HttpServletResponse".equals(className)
                || "org.springframework.web.multipart.MultipartFile".equals(className);
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
