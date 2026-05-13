/**
 * 数据权限 starter：提供数据范围注解、切面参数和当前用户解析扩展点。
 * <p>
 * 模块内按职责划分：
 * <ul>
 *   <li>根包：数据权限注解、类型、参数对象、切面和用户解析接口。</li>
 *   <li>{@code config}：数据权限相关自动配置。</li>
 * </ul>
 * 业务模块负责实现 {@code DataScopeUserResolver}，框架模块不反向依赖具体业务。
 */
package com.nz.admin.framework.datascope;
