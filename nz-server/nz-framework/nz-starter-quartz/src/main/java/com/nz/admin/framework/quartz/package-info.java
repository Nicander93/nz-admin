/**
 * Quartz 调度 starter：提供调度器装配、任务执行封装和调度扩展能力。
 * <p>
 * 模块内按职责划分：
 * <ul>
 *   <li>{@code config}：Quartz 自动配置和调度器装配。</li>
 *   <li>{@code job}：框架级 Job 基类和并发控制封装。</li>
 *   <li>{@code core}：后续扩展调度工具、任务日志 recorder 和 cron 工具。</li>
 * </ul>
 * 该模块只负责调度机制，不直接承载业务任务定义、任务表和执行记录表。
 */
package com.nz.admin.framework.quartz;
