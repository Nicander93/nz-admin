package com.nz.admin.framework.log.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.framework.log.annotation.Log;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

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

    @Resource
    private ApplicationContext applicationContext;

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
        Object requestAttributes = invokeStaticMethod(
                "org.springframework.web.context.request.RequestContextHolder",
                "getRequestAttributes",
                new Class<?>[0]
        );
        if (requestAttributes == null) {
            return;
        }
        Object request = invokeMethod(requestAttributes, "getRequest", new Class<?>[0]);
        if (request == null) {
            return;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Object operLog = newInstance("com.nz.admin.modules.system.entity.SysOperLog");
        if (operLog == null) {
            return;
        }

        setValue(operLog, "setTitle", String.class, log.title());
        setValue(operLog, "setBusinessType", int.class, log.businessType().getCode());
        setValue(operLog, "setOperContent", String.class, log.title() + "-" + log.businessType().getDesc());
        setValue(operLog, "setMethod", String.class, joinPoint.getTarget().getClass().getName() + "." + method.getName() + "()");
        setValue(operLog, "setRequestMethod", String.class, asString(invokeMethod(request, "getMethod", new Class<?>[0])));
        setValue(operLog, "setOperUrl", String.class, asString(invokeMethod(request, "getRequestURI", new Class<?>[0])));
        setValue(operLog, "setOperIp", String.class, resolveClientIp(request));
        setValue(operLog, "setOperParam", String.class, buildParam(joinPoint.getArgs()));
        setValue(operLog, "setJsonResult", String.class, result == null ? null : truncate(String.valueOf(result), 2000));
        setValue(operLog, "setStatus", int.class, ex == null ? 0 : 1);
        setValue(operLog, "setErrorMsg", String.class, ex == null ? null : truncate(ex.getMessage(), 2000));
        setValue(operLog, "setOperTime", LocalDateTime.class, LocalDateTime.now());
        setValue(operLog, "setOperName", String.class, resolveOperatorName());
        String userAgent = asString(invokeMethod(request, "getHeader", new Class<?>[]{String.class}, "User-Agent"));
        setValue(operLog, "setUserAgent", String.class, truncate(userAgent, 500));

        Object operLogService = getBean("com.nz.admin.modules.system.service.SysOperLogService");
        if (operLogService == null) {
            return;
        }
        Class<?> operLogClass = classForName("com.nz.admin.modules.system.entity.SysOperLog");
        if (operLogClass == null) {
            return;
        }
        invokeMethod(operLogService, "saveAsync", new Class<?>[]{operLogClass}, operLog);
    }

    private String resolveOperatorName() {
        if (!StpUtil.isLogin()) {
            return "anonymous";
        }
        Long userId = StpUtil.getLoginIdAsLong();
        Object userService = getBean("com.nz.admin.modules.system.service.SysUserService");
        if (userService == null) {
            return String.valueOf(userId);
        }
        Object user = invokeMethod(userService, "getById", new Class<?>[]{Long.class}, userId);
        if (user == null) {
            return String.valueOf(userId);
        }
        String username = asString(invokeMethod(user, "getUsername", new Class<?>[0]));
        return isBlank(username) ? String.valueOf(userId) : username;
    }

    private String buildParam(Object[] args) {
        List<Object> validArgs = new ArrayList<>();
        if (args == null || args.length == 0) {
            return "[]";
        }
        for (Object arg : args) {
            if (arg == null || shouldIgnore(arg)) {
                continue;
            }
            validArgs.add(arg);
        }
        return truncate(String.valueOf(validArgs), 2000);
    }

    private boolean shouldIgnore(Object arg) {
        String className = arg.getClass().getName();
        return "jakarta.servlet.http.HttpServletRequest".equals(className)
                || "jakarta.servlet.http.HttpServletResponse".equals(className)
                || "org.springframework.web.multipart.MultipartFile".equals(className);
    }

    private String resolveClientIp(Object request) {
        String forwarded = asString(invokeMethod(request, "getHeader", new Class<?>[]{String.class}, "X-Forwarded-For"));
        if (!isBlank(forwarded)) {
            int idx = forwarded.indexOf(',');
            return idx >= 0 ? forwarded.substring(0, idx).trim() : forwarded.trim();
        }
        String realIp = asString(invokeMethod(request, "getHeader", new Class<?>[]{String.class}, "X-Real-IP"));
        if (!isBlank(realIp)) {
            return realIp.trim();
        }
        return asString(invokeMethod(request, "getRemoteAddr", new Class<?>[0]));
    }

    private Object getBean(String className) {
        try {
            Class<?> clazz = classForName(className);
            if (clazz == null) {
                return null;
            }
            return applicationContext.getBean(clazz);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object newInstance(String className) {
        try {
            Class<?> clazz = classForName(className);
            if (clazz == null) {
                return null;
            }
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object invokeStaticMethod(String className, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> clazz = classForName(className);
            if (clazz == null) {
                return null;
            }
            Method method = clazz.getMethod(methodName, parameterTypes);
            return method.invoke(null, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Class<?> classForName(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object invokeMethod(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void setValue(Object target, String methodName, Class<?> parameterType, Object value) {
        invokeMethod(target, methodName, new Class<?>[]{parameterType}, value);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
