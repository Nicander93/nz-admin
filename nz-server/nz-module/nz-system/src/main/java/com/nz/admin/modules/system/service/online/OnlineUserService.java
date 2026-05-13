package com.nz.admin.modules.system.service.online;

import com.nz.admin.modules.system.entity.vo.online.OnlineUserVO;

import java.util.List;

/**
 * 在线用户服务。
 */
public interface OnlineUserService {

    /**
     * 获取当前在线用户列表。
     */
    List<OnlineUserVO> listOnlineUsers();

    /**
     * 强制下线指定 token。
     */
    void forceLogout(String tokenValue);
}
