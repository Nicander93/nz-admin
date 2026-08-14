package com.nz.admin.modules.system.service.message;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.realtime.core.RealtimeMessage;
import com.nz.admin.framework.realtime.core.RealtimePublisher;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.message.MessageDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.dto.message.MessageSendRequest;
import com.nz.admin.modules.system.entity.query.message.MessageQuery;
import com.nz.admin.modules.system.entity.vo.message.MessageVO;
import com.nz.admin.modules.system.mapper.message.MessageMapper;
import com.nz.admin.modules.system.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 站内消息服务实现。 */
@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, MessageDO>
        implements MessageService {

    private final UserService userService;
    private final Optional<RealtimePublisher> realtimePublisher;

    public MessageServiceImpl(UserService userService,
                              Optional<RealtimePublisher> realtimePublisher) {
        this.userService = userService;
        this.realtimePublisher = realtimePublisher;
    }

    @Override
    public Page<MessageDO> inbox(Long userId, MessageQuery query) {
        return baseMapper.selectInbox(query.toPage(), userId, query);
    }

    @Override
    public MessageVO getCurrent(Long userId, Long messageId) {
        return toVO(requireCurrent(userId, messageId));
    }

    @Override
    public long unreadCount(Long userId) {
        Long count = baseMapper.selectUnreadCount(userId);
        return count == null ? 0L : count;
    }

    @Override
    public void markRead(Long userId, Long messageId) {
        MessageDO message = requireCurrent(userId, messageId);
        if (Integer.valueOf(1).equals(message.getReadStatus())) {
            return;
        }
        update(new LambdaUpdateWrapper<MessageDO>()
                .set(MessageDO::getReadStatus, 1)
                .set(MessageDO::getReadTime, LocalDateTime.now())
                .eq(MessageDO::getId, messageId)
                .eq(MessageDO::getUserId, userId)
                .eq(MessageDO::getReadStatus, 0));
    }

    @Override
    public int markAllRead(Long userId) {
        return baseMapper.update(null, new LambdaUpdateWrapper<MessageDO>()
                .set(MessageDO::getReadStatus, 1)
                .set(MessageDO::getReadTime, LocalDateTime.now())
                .eq(MessageDO::getUserId, userId)
                .eq(MessageDO::getReadStatus, 0));
    }

    @Override
    public void removeCurrent(Long userId, Long messageId) {
        int removed = baseMapper.delete(new LambdaQueryWrapper<MessageDO>()
                .eq(MessageDO::getId, messageId)
                .eq(MessageDO::getUserId, userId));
        if (removed == 0) {
            throw new BusinessException("消息不存在");
        }
    }

    @Override
    @Transactional
    public int send(Long senderId, MessageSendRequest request) {
        validateRequest(request);
        List<UserDO> users = resolveReceivers(request);
        if (users.isEmpty()) {
            throw new BusinessException("没有可接收消息的启用用户");
        }
        Long tenantId = TenantContextHolder.getTenantIdOrNull();
        if (tenantId == null) {
            throw new BusinessException("当前租户上下文不存在");
        }
        String summary = StrUtil.blankToDefault(
                StrUtil.subPre(request.summary(), 500),
                StrUtil.subPre(request.content(), 200));
        List<MessageDO> messages = users.stream()
                .map(user -> new MessageDO()
                        .setTenantId(tenantId)
                        .setUserId(user.getId())
                        .setSenderId(senderId)
                        .setCategory(request.category())
                        .setType(StrUtil.blankToDefault(request.type(), "message"))
                        .setSource(StrUtil.blankToDefault(request.source(), "backend"))
                        .setTitle(request.title().trim())
                        .setSummary(summary)
                        .setContent(request.content())
                        .setDataJson(request.dataJson())
                        .setPath(request.path())
                        .setReadStatus(0))
                .toList();
        messages.forEach(baseMapper::insert);
        publishAfterCommit(tenantId, messages);
        return messages.size();
    }

    private List<UserDO> resolveReceivers(MessageSendRequest request) {
        if ("ALL".equals(request.targetType())) {
            return userService.listEnabledUsers(List.of());
        }
        Collection<Long> requestedIds = request.userIds() == null
                ? List.of() : new LinkedHashSet<>(request.userIds());
        if (requestedIds.isEmpty()) {
            throw new BusinessException("指定用户发送时必须选择接收人");
        }
        List<UserDO> users = userService.listEnabledUsers(requestedIds);
        if (users.size() != requestedIds.size()) {
            throw new BusinessException("部分接收人不存在、已禁用或不属于当前租户");
        }
        return users;
    }

    private void validateRequest(MessageSendRequest request) {
        if (StrUtil.isNotBlank(request.dataJson()) && !JSONUtil.isTypeJSON(request.dataJson())) {
            throw new BusinessException("扩展数据必须是有效 JSON");
        }
        String path = request.path();
        if (StrUtil.isNotBlank(path) && (!path.startsWith("/") || path.startsWith("//"))) {
            throw new BusinessException("跳转路径必须是站内绝对路径");
        }
    }

    private MessageDO requireCurrent(Long userId, Long messageId) {
        MessageDO message = getById(messageId);
        if (message == null || !userId.equals(message.getUserId())) {
            throw new BusinessException("消息不存在");
        }
        return message;
    }

    @Override
    public MessageVO toVO(MessageDO message) {
        Object data = StrUtil.isBlank(message.getDataJson())
                ? null : JSONUtil.parse(message.getDataJson());
        return new MessageVO(
                message.getId(), message.getCategory(), message.getType(),
                message.getSource(), message.getTitle(), message.getSummary(),
                message.getContent(), data, message.getPath(),
                message.getReadStatus(), message.getReadTime(), message.getCreateTime());
    }

    private void publishAfterCommit(Long tenantId, List<MessageDO> messages) {
        if (realtimePublisher.isEmpty()) {
            return;
        }
        Runnable publish = () -> messages.forEach(message -> publishOne(tenantId, message));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publish.run();
                        }
                    });
        } else {
            publish.run();
        }
    }

    private void publishOne(Long tenantId, MessageDO message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", message.getId());
        payload.put("category", message.getCategory());
        payload.put("title", message.getTitle());
        payload.put("summary", message.getSummary());
        if (StrUtil.isNotBlank(message.getPath())) {
            payload.put("path", message.getPath());
        }
        try {
            realtimePublisher.orElseThrow().publishToUser(
                    tenantId, message.getUserId(),
                    RealtimeMessage.of("system-message", payload));
        } catch (RuntimeException e) {
            log.warn("站内消息 {} 实时推送失败", message.getId(), e);
        }
    }
}
