package com.nz.admin.modules.workflow.controller;

import com.nz.admin.common.core.R;
import org.dromara.warm.flow.core.exception.FlowException;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 引擎业务校验失败也遵循统一响应格式，不作为未知系统错误展示。 */
@Order(-1)
@RestControllerAdvice(assignableTypes = {WorkflowEngineController.class, WorkflowDesignerController.class})
public class WorkflowEngineExceptionHandler {
    @ExceptionHandler(FlowException.class)
    public R<Void> invalidFlow(FlowException error) { return R.fail(error.getMessage()); }
}
