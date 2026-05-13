/**
 * system 模块：通用支撑能力（用户、部门、权限、字典、文件、任务、日志等）。
 * <p>
 * 按业务域划分子包，各层使用<strong>同一套域名称</strong>，便于对照：
 * <ul>
 *   <li>{@code auth} — 登录认证（仅 controller）</li>
 *   <li>{@code config} — 系统参数</li>
 *   <li>{@code dept} — 部门、岗位</li>
 *   <li>{@code dict} — 字典类型、字典数据</li>
 *   <li>{@code file} — 文件上传、存储策略、文件记录</li>
 *   <li>{@code job} — 定时任务、任务日志、任务日志下沉</li>
 *   <li>{@code log} — 登录日志、操作日志</li>
 *   <li>{@code menu} — 菜单</li>
 *   <li>{@code notice} — 通知公告</li>
 *   <li>{@code online} — 在线用户</li>
 *   <li>{@code permission} — 权限（菜单/角色分配等聚合能力）</li>
 *   <li>{@code role} — 角色、角色菜单</li>
 *   <li>{@code user} — 用户、用户角色</li>
 * </ul>
 * 典型路径（以域 {@code dept} 为例）：
 * {@code com.nz.admin.modules.system.controller.dept}、
 * {@code com.nz.admin.modules.system.service.dept}（接口与 {@code *ServiceImpl} 同包）、
 * {@code com.nz.admin.modules.system.mapper.dept}、
 * {@code com.nz.admin.modules.system.entity.dataobject.dept}、
 * {@code com.nz.admin.modules.system.entity.query} 下按域分子包等。
 */
package com.nz.admin.modules.system;
