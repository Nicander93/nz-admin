package com.nz.admin.modules.system.entity.vo.social;

/** 社交授权回调结果。 */
public record SocialCallbackVO(String purpose, String token, SocialBindingVO binding) {
}
