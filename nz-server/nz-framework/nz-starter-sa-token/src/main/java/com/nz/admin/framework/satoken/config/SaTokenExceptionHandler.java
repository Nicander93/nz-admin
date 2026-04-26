package com.nz.admin.framework.satoken.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.nz.admin.common.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 异常处理器。
 */
@RestControllerAdvice
public class SaTokenExceptionHandler {

    /**
     * 处理未登录异常。
     *
     * @param e 未登录异常
     * @return 统一响应
     */
    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNotLogin(NotLoginException e) {
        return R.fail(401, "未登录或登录已过期");
    }

    /**
     * 处理无权限异常。
     *
     * @param e 无权限异常
     * @return 统一响应
     */
    @ExceptionHandler(NotPermissionException.class)
    public R<Void> handleNotPermission(NotPermissionException e) {
        return R.fail(403, "没有访问权限");
    }

    /**
     * 处理无角色异常。
     *
     * @param e 无角色异常
     * @return 统一响应
     */
    @ExceptionHandler(NotRoleException.class)
    public R<Void> handleNotRole(NotRoleException e) {
        return R.fail(403, "没有访问权限");
    }
}
