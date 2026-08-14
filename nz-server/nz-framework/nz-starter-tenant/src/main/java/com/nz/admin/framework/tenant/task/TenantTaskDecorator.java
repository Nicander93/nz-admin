package com.nz.admin.framework.tenant.task;

import com.nz.admin.framework.tenant.core.TenantContextHolder;
import org.springframework.core.task.TaskDecorator;

/**
 * 把提交任务时的租户上下文带到异步线程，并在任务结束后清理。
 */
public class TenantTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Long tenantId = TenantContextHolder.getTenantIdOrNull();
        return () -> TenantContextHolder.runWithTenantId(tenantId, runnable);
    }
}
