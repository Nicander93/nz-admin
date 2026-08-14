package com.nz.admin.common.module;

import java.util.List;

/**
 * 跨业务模块发送用户通知的轻量命令。
 */
public record NzUserNotification(
        Long senderId,
        List<Long> receiverIds,
        List<String> receiverRoleKeys,
        String category,
        String type,
        String source,
        String title,
        String summary,
        String content,
        String dataJson,
        String path
) {
}
