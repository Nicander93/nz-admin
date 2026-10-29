package com.nz.admin.framework.realtime.core;

/** 事件的发送范围，用于区分个人、租户和全局投递。 */
public enum RealtimeDeliveryKind {
    USER,
    TENANT,
    BROADCAST
}
