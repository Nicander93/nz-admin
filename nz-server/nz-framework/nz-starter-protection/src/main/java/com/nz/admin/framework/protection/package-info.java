/**
 * 保护 starter：提供防重复提交、接口限流和输入清理基础能力。
 * <p>
 * 模块内按职责划分：
 * <ul>
 *   <li>{@code annotation}：声明保护类注解。</li>
 *   <li>{@code aspect}：重复提交与限流切面。</li>
 *   <li>{@code core}：本地限流器和 key 解析。</li>
 *   <li>{@code support}：输入清理工具。</li>
 * </ul>
 */
package com.nz.admin.framework.protection;
