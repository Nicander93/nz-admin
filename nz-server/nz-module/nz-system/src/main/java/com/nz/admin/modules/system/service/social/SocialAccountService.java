package com.nz.admin.modules.system.service.social;

import com.nz.admin.framework.social.core.SocialAuthorization;
import com.nz.admin.framework.social.core.SocialProvider;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.vo.social.SocialBindingVO;
import com.nz.admin.modules.system.entity.vo.social.SocialCallbackVO;
import com.nz.admin.modules.system.service.auth.AuthenticationService;

import java.util.List;

/** 第三方账号登录与绑定业务。 */
public interface SocialAccountService {
    List<SocialProvider> providers();

    SocialAuthorization authorizeLogin(TenantDO tenant, String clientId, String provider);

    SocialAuthorization authorizeBinding(Long tenantId, Long userId, String provider);

    SocialCallbackVO callback(String provider, String code, String state,
                              AuthenticationService.LoginMetadata metadata);

    List<SocialBindingVO> listCurrent(Long userId);

    void unbind(Long userId, Long bindingId);
}
