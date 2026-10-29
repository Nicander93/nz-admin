package com.nz.admin.common.job;

/** 业务侧持久执行边界；false 表示已完成、已跳过或正由其他节点执行。 */
public interface JobExecutionCoordinator {
    boolean execute(JobInvocation invocation, Runnable action);
    default boolean recordsFailures() { return false; }
}
