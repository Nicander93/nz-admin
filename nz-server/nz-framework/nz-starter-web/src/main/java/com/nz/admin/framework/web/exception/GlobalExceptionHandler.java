package com.nz.admin.framework.web.exception;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.core.CommonErrorCode;
import com.nz.admin.common.core.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
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

    @Autowired(required = false)
    private ApiExceptionLogRecorder apiExceptionLogRecorder;

    /**
     * 处理请求体校验异常。
     *
     * @param e 校验异常
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        return R.fail(CommonErrorCode.VALIDATION_ERROR.getCode(),
                fieldError == null ? CommonErrorCode.VALIDATION_ERROR.getMessage() : fieldError.getDefaultMessage());
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
        return R.fail(CommonErrorCode.VALIDATION_ERROR.getCode(),
                fieldError == null ? CommonErrorCode.VALIDATION_ERROR.getMessage() : fieldError.getDefaultMessage());
    }

    /**
     * 处理方法参数校验异常。
     *
     * @param e 校验异常
     * @return 统一响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        return R.fail(CommonErrorCode.VALIDATION_ERROR.getCode(),
                Objects.requireNonNullElse(e.getMessage(), CommonErrorCode.VALIDATION_ERROR.getMessage()));
    }

    /**
     * 处理缺失请求参数异常。
     *
     * @param e 参数异常
     * @return 统一响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        return R.fail(CommonErrorCode.BAD_REQUEST.getCode(), e.getParameterName() + " 不能为空");
    }

    /**
     * 处理业务异常。
     *
     * @param e 业务异常
     * @return 统一响应
     */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        return R.fail(e);
    }

    /**
     * 处理业务参数异常。
     *
     * @param e 参数异常
     * @return 统一响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(CommonErrorCode.BAD_REQUEST.getCode(),
                Objects.requireNonNullElse(e.getMessage(), CommonErrorCode.BAD_REQUEST.getMessage()));
    }

    /**
     * 处理兜底异常。
     *
     * @param e 系统异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常", e);
        if (apiExceptionLogRecorder != null && request != null) {
            try {
                apiExceptionLogRecorder.record(e, request);
            } catch (Exception ex) {
                log.warn("记录接口异常日志失败", ex);
            }
        }
        return R.fail(CommonErrorCode.SYSTEM_ERROR);
    }
}
