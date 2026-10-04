package com.nz.admin.common.module;

/** 业务事件按实例顺序至少投递一次；处理方必须按 instanceId + sequence 去重。 */
public interface NzWorkflowBusinessHandler {
    String businessType();
    default String businessName() { return businessType(); }
    void apply(Event event);
    /** 仅在运行时校验实例参与人后调用，返回审批所需业务字段。 */
    default java.util.Map<String, Object> detail(String tenantId, String businessId) { return java.util.Map.of(); }
    record Event(String tenantId, String businessId, String instanceId, long sequence,
                 String flowStatus, int nodeType, String actor, String operation) {}
}
