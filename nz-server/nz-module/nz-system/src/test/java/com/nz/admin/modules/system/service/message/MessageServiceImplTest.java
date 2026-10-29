package com.nz.admin.modules.system.service.message;

import com.nz.admin.common.core.BusinessException;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.nz.admin.framework.realtime.core.RealtimeMessage;
import com.nz.admin.framework.realtime.core.RealtimePublisher;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.message.MessageDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.dto.message.MessageSendRequest;
import com.nz.admin.modules.system.mapper.message.MessageMapper;
import com.nz.admin.modules.system.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MessageServiceImplTest {

    private UserService userService;
    private RealtimePublisher realtimePublisher;
    private MessageMapper messageMapper;
    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), MessageDO.class);
        userService = mock(UserService.class);
        realtimePublisher = mock(RealtimePublisher.class);
        messageMapper = mock(MessageMapper.class);
        service = new MessageServiceImpl(userService, Optional.of(realtimePublisher));
        ReflectionTestUtils.setField(service, "baseMapper", messageMapper);
    }

    @Test
    void sendsOneTenantScopedMessagePerEnabledReceiverAndPublishesRealtimeEvent() {
        when(userService.listEnabledUsers(List.of())).thenReturn(List.of(
                new UserDO().setId(11L),
                new UserDO().setId(12L)
        ));
        when(messageMapper.insert(any(MessageDO.class))).thenAnswer(invocation -> {
            MessageDO message = invocation.getArgument(0);
            message.setId(message.getUserId() + 100);
            return 1;
        });

        int count = TenantContextHolder.callWithTenantId(
                9L, () -> service.send(7L, request("ALL", List.of(), null, "/system/message")));

        assertThat(count).isEqualTo(2);
        ArgumentCaptor<MessageDO> messages = ArgumentCaptor.forClass(MessageDO.class);
        verify(messageMapper, times(2)).insert(messages.capture());
        assertThat(messages.getAllValues())
                .extracting(MessageDO::getTenantId, MessageDO::getSenderId, MessageDO::getReadStatus)
                .containsOnly(tuple(9L, 7L, 0));
        verify(realtimePublisher, times(2)).publishToUser(
                eq(9L), any(Long.class), any(RealtimeMessage.class));
    }

    @Test
    void rejectsMissingOrDisabledSelectedReceiver() {
        when(userService.listEnabledUsers(any())).thenReturn(List.of(new UserDO().setId(11L)));

        assertThatThrownBy(() -> TenantContextHolder.callWithTenantId(
                9L, () -> service.send(7L, request("USERS", List.of(11L, 12L), null, null))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("部分接收人不存在、已禁用或不属于当前租户");
        verifyNoInteractions(messageMapper);
    }

    @Test
    void rejectsUnsafePathAndInvalidJsonBeforeResolvingReceivers() {
        assertThatThrownBy(() -> service.send(
                7L, request("ALL", List.of(), "{broken", "//external.test")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("扩展数据必须是有效 JSON");
        verifyNoInteractions(userService, messageMapper, realtimePublisher);
    }

    @Test
    void preventsReadingAnotherUsersMessage() {
        when(messageMapper.selectById(21L))
                .thenReturn(new MessageDO().setId(21L).setUserId(12L));

        assertThatThrownBy(() -> service.getCurrent(11L, 21L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("消息不存在");
    }

    @Test
    void rejectedInsertDoesNotPublishPartiallyCreatedBatch() {
        when(userService.listEnabledUsers(List.of())).thenReturn(List.of(
                new UserDO().setId(11L), new UserDO().setId(12L)));
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        when(messageMapper.insert(any(MessageDO.class))).thenAnswer(call -> {
            call.<MessageDO>getArgument(0).setId(100L);
            return calls.incrementAndGet() == 1 ? 1 : 0;
        });
        assertThatThrownBy(() -> TenantContextHolder.callWithTenantId(9L,
                () -> service.send(7L, request("ALL", List.of(), null, null))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("消息写入失败");
        verifyNoInteractions(realtimePublisher);
    }

    @Test
    void publishesReadStateOnlyWhenTheOwnedRowChanges() {
        when(messageMapper.selectById(21L)).thenReturn(new MessageDO().setId(21L)
                .setTenantId(9L).setUserId(11L).setReadStatus(0));
        when(messageMapper.update(isNull(), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(1, 0);
        TenantContextHolder.runWithTenantId(9L, () -> {
            service.markRead(11L, 21L);
            service.markRead(11L, 21L);
        });
        ArgumentCaptor<RealtimeMessage> event = ArgumentCaptor.forClass(RealtimeMessage.class);
        verify(realtimePublisher).publishToUser(eq(9L), eq(11L), event.capture());
        assertThat(event.getValue().type()).isEqualTo("system-message-state");
        assertThat(event.getValue().payload()).isEqualTo(java.util.Map.of("action", "READ", "id", 21L));
    }

    @Test
    void alreadyReadInboxDoesNotPublishRedundantStateEvents() {
        when(messageMapper.update(isNull(), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(0);
        assertThat(TenantContextHolder.callWithTenantId(9L, () -> service.markAllRead(11L))).isZero();
        verifyNoInteractions(realtimePublisher);
    }

    private MessageSendRequest request(String targetType, List<Long> userIds,
                                       String dataJson, String path) {
        return new MessageSendRequest(
                "notice", null, null, "维护通知", null,
                "系统将在今晚维护。", dataJson, path, targetType, userIds
        );
    }
}
