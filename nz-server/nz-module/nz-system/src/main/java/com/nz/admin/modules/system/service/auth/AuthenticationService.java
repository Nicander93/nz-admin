package com.nz.admin.modules.system.service.auth;

import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;

/** 账号、短信与第三方授权登录服务。 */
public interface AuthenticationService {

    String loginByPassword(TenantDO tenant, String clientId, String username,
                           String password, LoginMetadata metadata);

    void sendSmsLoginCode(String clientId, String phone);

    String loginBySms(TenantDO tenant, String clientId, String phone,
                      String code, LoginMetadata metadata);

    String loginBySocial(TenantDO tenant, String clientId, UserDO user,
                         LoginMetadata metadata);

    /** 登录请求中可信的服务端元数据。 */
    record LoginMetadata(String ip, String userAgent) {
    }
}
