package com.nz.admin.common.module;

/**
 * 用户通知发布协议，由消息模块提供实现。
 */
public interface NzUserNotificationPublisher {

    int publish(NzUserNotification notification);
}
