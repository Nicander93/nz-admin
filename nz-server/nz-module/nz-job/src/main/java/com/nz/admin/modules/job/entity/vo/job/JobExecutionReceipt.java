package com.nz.admin.modules.job.entity.vo.job;

/** 排队回执不是任务成功结果，可按 executionId/key 查询终态。 */
public record JobExecutionReceipt(Long executionId, String executionKey, String state) {}
