package com.nz.admin.modules.system.controller.message;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.dataobject.message.MessageDO;
import com.nz.admin.modules.system.entity.dto.message.MessageSendRequest;
import com.nz.admin.modules.system.entity.query.message.MessageQuery;
import com.nz.admin.modules.system.entity.vo.message.MessageVO;
import com.nz.admin.modules.system.service.message.MessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 当前用户消息中心和管理员发送接口。 */
@RestController
@RequestMapping("/api/system/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @SaCheckPermission("system:message:list")
    @GetMapping("/page")
    public R<PageResult<MessageVO>> page(MessageQuery query) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<MessageDO> page = messageService.inbox(userId, query);
        List<MessageVO> records = page.getRecords().stream()
                .map(messageService::toVO).toList();
        return R.ok(PageResult.of(page, records));
    }

    @SaCheckPermission("system:message:query")
    @GetMapping("/{messageId}")
    public R<MessageVO> get(@PathVariable Long messageId) {
        return R.ok(messageService.getCurrent(StpUtil.getLoginIdAsLong(), messageId));
    }

    @SaCheckPermission("system:message:list")
    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(messageService.unreadCount(StpUtil.getLoginIdAsLong()));
    }

    @SaCheckPermission("system:message:read")
    @PutMapping("/{messageId}/read")
    public R<Void> markRead(@PathVariable Long messageId) {
        messageService.markRead(StpUtil.getLoginIdAsLong(), messageId);
        return R.ok();
    }

    @SaCheckPermission("system:message:read")
    @PutMapping("/read-all")
    public R<Integer> markAllRead() {
        return R.ok(messageService.markAllRead(StpUtil.getLoginIdAsLong()));
    }

    @Log(title = "消息中心", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:message:remove")
    @DeleteMapping("/{messageId}")
    public R<Void> remove(@PathVariable Long messageId) {
        messageService.removeCurrent(StpUtil.getLoginIdAsLong(), messageId);
        return R.ok();
    }

    @Log(title = "消息中心", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:message:send")
    @PostMapping("/send")
    public R<Integer> send(@Valid @RequestBody MessageSendRequest request) {
        return R.ok(messageService.send(StpUtil.getLoginIdAsLong(), request));
    }
}
