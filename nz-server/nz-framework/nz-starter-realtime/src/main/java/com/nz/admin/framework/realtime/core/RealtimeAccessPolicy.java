package com.nz.admin.framework.realtime.core;

/** 由业务模块提供的连接授权与事件投递策略。 */
public interface RealtimeAccessPolicy {

    boolean isAuthorized(RealtimePrincipal principal);

    boolean canReceive(
            RealtimePrincipal principal,
            RealtimeDeliveryKind deliveryKind,
            RealtimeMessage message);

    /** 手工创建的 standalone realtime 核心只开放原有通用范围。 */
    static RealtimeAccessPolicy generalOnly() {
        return new RealtimeAccessPolicy() {
            @Override
            public boolean isAuthorized(RealtimePrincipal principal) {
                return principal != null && principal.scope() == RealtimeScope.GENERAL;
            }

            @Override
            public boolean canReceive(
                    RealtimePrincipal principal,
                    RealtimeDeliveryKind deliveryKind,
                    RealtimeMessage message) {
                return isAuthorized(principal);
            }
        };
    }

    /** Spring 自动装配没有业务授权策略时默认拒绝全部连接。 */
    static RealtimeAccessPolicy denyAll() {
        return new RealtimeAccessPolicy() {
            @Override
            public boolean isAuthorized(RealtimePrincipal principal) {
                return false;
            }

            @Override
            public boolean canReceive(
                    RealtimePrincipal principal,
                    RealtimeDeliveryKind deliveryKind,
                    RealtimeMessage message) {
                return false;
            }
        };
    }
}
