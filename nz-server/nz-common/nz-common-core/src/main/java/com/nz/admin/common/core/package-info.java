/**
 * 公共核心模块：放跨模块共享的基础对象和轻量协议。
 * <p>
 * 当前主要包括：
 * <ul>
 *   <li>{@code BaseEntity}：通用实体基类。</li>
 *   <li>{@code PageQuery}：分页查询参数。</li>
 *   <li>{@code R}：接口统一响应对象。</li>
 * </ul>
 * 该包不放业务逻辑，也不依赖框架 starter 或业务模块，保持轻量、稳定、可复用。
 */
package com.nz.admin.common.core;
