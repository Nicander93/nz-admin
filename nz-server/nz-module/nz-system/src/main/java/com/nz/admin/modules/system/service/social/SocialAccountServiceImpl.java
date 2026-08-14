package com.nz.admin.modules.system.service.social;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.social.core.SocialAuthenticationException;
import com.nz.admin.framework.social.core.SocialAuthorization;
import com.nz.admin.framework.social.core.SocialAuthorizationContext;
import com.nz.admin.framework.social.core.SocialCallbackResult;
import com.nz.admin.framework.social.core.SocialIdentity;
import com.nz.admin.framework.social.core.SocialOAuthService;
import com.nz.admin.framework.social.core.SocialProvider;
import com.nz.admin.framework.tenant.core.TenantConstants;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.social.SocialBindingDO;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.vo.social.SocialBindingVO;
import com.nz.admin.modules.system.entity.vo.social.SocialCallbackVO;
import com.nz.admin.modules.system.mapper.social.SocialBindingMapper;
import com.nz.admin.modules.system.service.auth.AuthenticationService;
import com.nz.admin.modules.system.service.client.ClientService;
import com.nz.admin.modules.system.service.tenant.TenantService;
import com.nz.admin.modules.system.service.user.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 第三方账号登录与绑定业务实现。 */
@Service
public class SocialAccountServiceImpl
        extends ServiceImpl<SocialBindingMapper, SocialBindingDO>
        implements SocialAccountService {

    private static final String PURPOSE_LOGIN = "LOGIN";
    private static final String PURPOSE_BIND = "BIND";

    private final SocialOAuthService socialOAuthService;
    private final ClientService clientService;
    private final TenantService tenantService;
    private final UserService userService;
    private final AuthenticationService authenticationService;

    public SocialAccountServiceImpl(SocialOAuthService socialOAuthService,
                                    ClientService clientService,
                                    TenantService tenantService,
                                    UserService userService,
                                    AuthenticationService authenticationService) {
        this.socialOAuthService = socialOAuthService;
        this.clientService = clientService;
        this.tenantService = tenantService;
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    @Override
    public List<SocialProvider> providers() {
        return socialOAuthService.providers();
    }

    @Override
    public SocialAuthorization authorizeLogin(TenantDO tenant, String clientId, String provider) {
        clientService.getEnabledForLogin(clientId, "social");
        return authorize(provider,
                new SocialAuthorizationContext(tenant.getId(), PURPOSE_LOGIN, clientId, null));
    }

    @Override
    public SocialAuthorization authorizeBinding(Long tenantId, Long userId, String provider) {
        return authorize(provider,
                new SocialAuthorizationContext(tenantId, PURPOSE_BIND, null, userId));
    }

    private SocialAuthorization authorize(String provider, SocialAuthorizationContext context) {
        try {
            return socialOAuthService.authorize(provider, context);
        } catch (SocialAuthenticationException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public SocialCallbackVO callback(String provider, String code, String state,
                                     AuthenticationService.LoginMetadata metadata) {
        SocialCallbackResult result;
        try {
            result = socialOAuthService.callback(provider, code, state);
        } catch (SocialAuthenticationException e) {
            throw new BusinessException(e.getMessage());
        }
        if (PURPOSE_LOGIN.equals(result.context().purpose())) {
            return completeLogin(result, metadata);
        }
        if (PURPOSE_BIND.equals(result.context().purpose())) {
            return completeBinding(result);
        }
        throw new BusinessException("第三方授权用途无效");
    }

    private SocialCallbackVO completeLogin(SocialCallbackResult result,
                                           AuthenticationService.LoginMetadata metadata) {
        SocialAuthorizationContext context = result.context();
        return TenantContextHolder.callWithTenantId(context.tenantId(), () -> {
            SocialBindingDO binding = findByIdentity(result.identity());
            if (binding == null) {
                throw new BusinessException("该第三方账号尚未绑定系统用户");
            }
            UserDO user = userService.getById(binding.getUserId());
            if (user == null) {
                throw new BusinessException("绑定的系统用户不存在");
            }
            TenantDO tenant = tenantService.getRequired(context.tenantId());
            String token = authenticationService.loginBySocial(
                    tenant, context.clientId(), user, metadata);
            return new SocialCallbackVO(PURPOSE_LOGIN, token, toVO(binding));
        });
    }

    private SocialCallbackVO completeBinding(SocialCallbackResult result) {
        SocialAuthorizationContext context = result.context();
        validateBindingSession(context);
        return TenantContextHolder.callWithTenantId(context.tenantId(), () -> {
            SocialBindingDO binding = saveBinding(context.userId(), result.identity());
            return new SocialCallbackVO(PURPOSE_BIND, null, toVO(binding));
        });
    }

    private void validateBindingSession(SocialAuthorizationContext context) {
        if (!StpUtil.isLogin() || !Objects.equals(StpUtil.getLoginIdAsLong(), context.userId())) {
            throw new BusinessException("绑定登录态已失效，请重新登录");
        }
        Object tenantValue = StpUtil.getTokenSession().get(TenantConstants.TOKEN_SESSION_TENANT_ID);
        Long sessionTenantId = tenantValue instanceof Number number ? number.longValue() : null;
        if (!Objects.equals(sessionTenantId, context.tenantId())) {
            throw new BusinessException("绑定租户与当前登录态不一致");
        }
    }

    private SocialBindingDO saveBinding(Long userId, SocialIdentity identity) {
        SocialBindingDO identityBinding = findByIdentity(identity);
        if (identityBinding != null && !Objects.equals(identityBinding.getUserId(), userId)) {
            throw new BusinessException("该第三方账号已绑定其他系统用户");
        }
        SocialBindingDO binding = getOne(new LambdaQueryWrapper<SocialBindingDO>()
                .eq(SocialBindingDO::getUserId, userId)
                .eq(SocialBindingDO::getProvider, identity.provider()));
        if (binding == null) {
            binding = new SocialBindingDO().setUserId(userId)
                    .setProvider(identity.provider())
                    .setProviderUserId(identity.providerUserId());
        }
        copyProfile(binding, identity);
        try {
            saveOrUpdate(binding);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("第三方账号绑定冲突，请刷新后重试");
        }
        return binding;
    }

    private SocialBindingDO findByIdentity(SocialIdentity identity) {
        return getOne(new LambdaQueryWrapper<SocialBindingDO>()
                .eq(SocialBindingDO::getProvider, identity.provider())
                .eq(SocialBindingDO::getProviderUserId, identity.providerUserId()));
    }

    private void copyProfile(SocialBindingDO binding, SocialIdentity identity) {
        binding.setProviderUserId(identity.providerUserId())
                .setUsername(StrUtil.subPre(identity.username(), 200))
                .setNickname(StrUtil.subPre(identity.nickname(), 200))
                .setEmail(StrUtil.subPre(identity.email(), 320))
                .setAvatar(StrUtil.subPre(identity.avatar(), 1000));
    }

    @Override
    public List<SocialBindingVO> listCurrent(Long userId) {
        return list(new LambdaQueryWrapper<SocialBindingDO>()
                .eq(SocialBindingDO::getUserId, userId)
                .orderByDesc(SocialBindingDO::getCreateTime))
                .stream().map(this::toVO).toList();
    }

    @Override
    public void unbind(Long userId, Long bindingId) {
        SocialBindingDO binding = getById(bindingId);
        if (binding == null || !Objects.equals(binding.getUserId(), userId)) {
            throw new BusinessException("第三方账号绑定不存在");
        }
        removeById(bindingId);
    }

    private SocialBindingVO toVO(SocialBindingDO binding) {
        Map<String, SocialProvider> providers = providers().stream()
                .collect(Collectors.toMap(SocialProvider::code, Function.identity()));
        SocialProvider provider = providers.get(binding.getProvider());
        String providerName = provider == null ? binding.getProvider() : provider.displayName();
        return new SocialBindingVO(binding.getId(), binding.getProvider(), providerName,
                binding.getUsername(), binding.getNickname(), binding.getEmail(),
                binding.getAvatar(), binding.getCreateTime());
    }
}
