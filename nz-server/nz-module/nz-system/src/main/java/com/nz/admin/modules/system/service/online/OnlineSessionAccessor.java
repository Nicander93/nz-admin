package com.nz.admin.modules.system.service.online;

import java.util.List;

/**
 * 在线会话访问边界，隔离业务服务与 Sa-Token 静态 API。
 */
public interface OnlineSessionAccessor {

    List<String> listTokenValues();

    OnlineSessionSnapshot getSnapshot(String tokenValue);

    void logout(String tokenValue);
}
