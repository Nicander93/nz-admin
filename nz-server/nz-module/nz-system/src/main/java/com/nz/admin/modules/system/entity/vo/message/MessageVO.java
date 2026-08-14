package com.nz.admin.modules.system.entity.vo.message;

import java.time.LocalDateTime;

/** 消息中心展示数据。 */
public record MessageVO(
        Long id,
        String category,
        String type,
        String source,
        String title,
        String summary,
        String content,
        Object data,
        String path,
        Integer readStatus,
        LocalDateTime readTime,
        LocalDateTime createTime
) {
}
