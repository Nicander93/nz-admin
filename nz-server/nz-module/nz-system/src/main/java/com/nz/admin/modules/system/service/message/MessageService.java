package com.nz.admin.modules.system.service.message;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.message.MessageDO;
import com.nz.admin.modules.system.entity.dto.message.MessageSendRequest;
import com.nz.admin.modules.system.entity.query.message.MessageQuery;
import com.nz.admin.modules.system.entity.vo.message.MessageVO;

/** 站内消息收件箱和发送服务。 */
public interface MessageService {
    Page<MessageDO> inbox(Long userId, MessageQuery query);

    MessageVO getCurrent(Long userId, Long messageId);

    long unreadCount(Long userId);

    void markRead(Long userId, Long messageId);

    int markAllRead(Long userId);

    void removeCurrent(Long userId, Long messageId);

    int send(Long senderId, MessageSendRequest request);

    MessageVO toVO(MessageDO message);
}
