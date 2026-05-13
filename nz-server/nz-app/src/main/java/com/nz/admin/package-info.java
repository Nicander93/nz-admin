/**
 * 应用启动模块：放后端进程入口、启动期装配和运行环境相关配置。
 * <p>
 * 该模块负责把业务模块和框架 starter 组合成可运行应用，常见内容包括：
 * <ul>
 *   <li>{@code NzAdminApplication}：Spring Boot 启动入口。</li>
 *   <li>{@code config}：启动期配置和基础数据初始化。</li>
 * </ul>
 * 这里不沉淀业务规则，业务实现优先放在 {@code nz-module}，通用框架能力优先放在 {@code nz-framework}。
 */
package com.nz.admin;
