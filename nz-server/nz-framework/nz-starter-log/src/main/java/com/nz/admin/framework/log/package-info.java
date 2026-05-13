/**
 * 操作日志 starter：提供日志注解、采集切面、默认配置和日志记录扩展点。
 * <p>
 * 模块内按职责划分：
 * <ul>
 *   <li>{@code annotation}：声明操作日志和业务类型。</li>
 *   <li>{@code aspect}：采集请求、返回值、异常和耗时等日志事件。</li>
 *   <li>{@code config}：日志相关自动配置。</li>
 *   <li>{@code core}：日志事件、脱敏和 recorder 扩展接口。</li>
 * </ul>
 * 框架层只负责采集和扩展点定义，具体日志落库、业务查询和页面展示放在业务模块。
 */
package com.nz.admin.framework.log;
