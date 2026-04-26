package com.nz.admin.framework.web.config;

import com.nz.admin.common.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * 全局异常处理器。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理请求体校验异常。
     *
     * @param e 校验异常
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        return R.fail(fieldError == null ? "请求参数校验失败" : fieldError.getDefaultMessage());
    }

    /**
     * 处理表单绑定校验异常。
     *
     * @param e 校验异常
     * @return 统一响应
     */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        return R.fail(fieldError == null ? "请求参数校验失败" : fieldError.getDefaultMessage());
    }

    /**
     * 处理方法参数校验异常。
     *
     * @param e 校验异常
     * @return 统一响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        return R.fail(Objects.requireNonNullElse(e.getMessage(), "请求参数校验失败"));
    }

    /**
     * 处理业务参数异常。
     *
     * @param e 参数异常
     * @return 统一响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(e.getMessage());
    }

    /**
     * 处理兜底异常。
     *
     * @param e 系统异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(e.getMessage());
    }
}
