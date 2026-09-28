package com.nz.admin.modules.workflow.handler;

import cn.hutool.core.util.StrUtil;
import org.dromara.warm.flow.core.entity.RootEntity;
import org.dromara.warm.flow.core.handler.DataFillHandler;
import org.dromara.warm.flow.core.handler.TenantHandler;
import org.springframework.stereotype.Component;

/**
 * 填充 Warm-Flow 的审计字段和租户字段。
 */
@Component
public class NzWarmFlowDataFillHandler implements DataFillHandler {

    private final TenantHandler tenantHandler;

    public NzWarmFlowDataFillHandler(TenantHandler tenantHandler) {
        this.tenantHandler = tenantHandler;
    }

    @Override
    public void insertFill(Object object) {
        if (!(object instanceof RootEntity entity)) {
            return;
        }
        DataFillHandler.super.insertFill(entity);
        if (StrUtil.isBlank(entity.getTenantId())) {
            entity.setTenantId(tenantHandler.getTenantId());
        }
    }
}
