/**
 * 公共任务契约模块：定义任务执行和任务日志下沉的跨模块协议。
 * <p>
 * 当前主要包括：
 * <ul>
 *   <li>{@code JobExecuteService}：任务执行入口，供调度侧触发业务任务。</li>
 *   <li>{@code JobInvokeLogEvent}：任务执行日志事件。</li>
 *   <li>{@code JobInvokeLogSink}：任务日志下沉扩展点。</li>
 * </ul>
 * 这里保留接口和事件对象，不直接实现具体系统任务。
 */
package com.nz.admin.common.job;
