package com.nz.admin.modules.system.service.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.module.NzUserNotification;
import com.nz.admin.common.module.NzUserNotificationPublisher;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserRoleDO;
import com.nz.admin.modules.system.entity.dto.message.MessageSendRequest;
import com.nz.admin.modules.system.mapper.role.RoleMapper;
import com.nz.admin.modules.system.mapper.user.UserRoleMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 把跨模块用户通知转换为系统站内消息。
 */
@Component
public class SystemUserNotificationPublisher implements NzUserNotificationPublisher {

    private final MessageService messageService;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public SystemUserNotificationPublisher(MessageService messageService,
                                           RoleMapper roleMapper,
                                           UserRoleMapper userRoleMapper) {
        this.messageService = messageService;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public int publish(NzUserNotification notification) {
        Set<Long> receiverIds = new LinkedHashSet<>();
        if (notification.receiverIds() != null) {
            receiverIds.addAll(notification.receiverIds());
        }
        addRoleUsers(receiverIds, notification.receiverRoleKeys());
        receiverIds.remove(notification.senderId());
        if (receiverIds.isEmpty()) {
            throw new BusinessException("当前任务没有可接收催办消息的用户");
        }
        return messageService.send(notification.senderId(), new MessageSendRequest(
                notification.category(),
                notification.type(),
                notification.source(),
                notification.title(),
                notification.summary(),
                notification.content(),
                notification.dataJson(),
                notification.path(),
                "USERS",
                List.copyOf(receiverIds)
        ));
    }

    private void addRoleUsers(Set<Long> receiverIds, List<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return;
        }
        List<Long> roleIds = roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                        .in(RoleDO::getRoleKey, new LinkedHashSet<>(roleKeys))
                        .eq(RoleDO::getStatus, 0))
                .stream().map(RoleDO::getId).toList();
        if (roleIds.isEmpty()) {
            return;
        }
        userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleDO>()
                        .in(UserRoleDO::getRoleId, roleIds))
                .stream().map(UserRoleDO::getUserId).forEach(receiverIds::add);
    }
}
