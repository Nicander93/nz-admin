package com.nz.admin.framework.log.aspect;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.SysOperLog;
import com.nz.admin.modules.system.entity.SysUser;
import com.nz.admin.modules.system.service.SysOperLogService;
import com.nz.admin.modules.system.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
@Component
public class LogAspect {

    @Autowired
    private SysOperLogService operLogService;

    @Autowired
    private SysUserService userService;

    /**
     * 拦截带日志注解的方法，记录成功或失败操作。
     */
    @Around("@annotation(log)")
    public Object around(ProceedingJoinPoint joinPoint, Log log) throws Throwable {
        Throwable ex = null;
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            ex = e;
            throw e;
        } finally {
            saveOperLog(joinPoint, log, result, ex);
        }
    }

    private void saveOperLog(ProceedingJoinPoint joinPoint, Log log, Object result, Throwable ex) {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        SysOperLog operLog = new SysOperLog();
        operLog.setTitle(log.title());
        operLog.setBusinessType(log.businessType().getCode());
        operLog.setOperContent(log.title() + "-" + log.businessType().getDesc());
        operLog.setMethod(joinPoint.getTarget().getClass().getName() + "." + method.getName() + "()");
        operLog.setRequestMethod(request.getMethod());
        operLog.setOperUrl(request.getRequestURI());
        operLog.setOperIp(resolveClientIp(request));
        operLog.setOperParam(buildParam(joinPoint.getArgs()));
        operLog.setJsonResult(result == null ? null : StrUtil.subPre(String.valueOf(result), 2000));
        operLog.setStatus(ex == null ? 0 : 1);
        operLog.setErrorMsg(ex == null ? null : StrUtil.subPre(ex.getMessage(), 2000));
        operLog.setOperTime(LocalDateTime.now());
        operLog.setOperName(resolveOperatorName());
        operLog.setUserAgent(StrUtil.subPre(request.getHeader("User-Agent"), 500));

        // 异步保存日志
        operLogService.saveAsync(operLog);
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private String resolveOperatorName() {
        if (!StpUtil.isLogin()) {
            return "anonymous";
        }
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userService.getById(userId);
        if (user == null) {
            return String.valueOf(userId);
        }
        return StrUtil.blankToDefault(user.getUsername(), String.valueOf(userId));
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
        return StrUtil.subPre(JSONUtil.toJsonStr(validArgs), 2000);
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
