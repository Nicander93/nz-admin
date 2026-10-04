package com.nz.admin.common.module;

import java.util.Map;

/** 业务服务调用，业务类型由服务端指定；调用前需校验单据归属和可提交状态。 */
public interface NzWorkflowBusinessLauncher {
    java.util.List<Definition> available(String businessType);
    record Definition(String flowCode, String flowName) {}
    String start(String businessType, String businessId, String flowCode, Map<String, Object> variables);
}
