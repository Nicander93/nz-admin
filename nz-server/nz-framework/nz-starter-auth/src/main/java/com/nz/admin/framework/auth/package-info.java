/**
 * 认证鉴权 starter：提供 Sa-Token 接入、权限注解、鉴权切面和异常处理。
 * <p>
 * 模块内按职责划分：
 * <ul>
 *   <li>{@code annotation}：权限校验注解和校验模式。</li>
 *   <li>{@code aspect}：解析注解并调用 Sa-Token 完成权限判断。</li>
 *   <li>{@code config}：登录拦截器和鉴权异常处理。</li>
 * </ul>
 * 该模块只提供框架级认证鉴权能力，不绑定具体用户、角色、菜单等业务表。
 */
package com.nz.admin.framework.auth;
