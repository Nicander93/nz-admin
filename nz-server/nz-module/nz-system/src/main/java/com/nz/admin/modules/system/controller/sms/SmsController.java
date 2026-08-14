package com.nz.admin.modules.system.controller.sms;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.framework.sms.core.SmsGateway;
import com.nz.admin.modules.system.entity.dto.sms.SmsChannelSaveRequest;
import com.nz.admin.modules.system.entity.dto.sms.SmsTemplateSaveRequest;
import com.nz.admin.modules.system.entity.dto.sms.SmsTestSendRequest;
import com.nz.admin.modules.system.entity.vo.sms.SmsChannelVO;
import com.nz.admin.modules.system.entity.vo.sms.SmsSendLogVO;
import com.nz.admin.modules.system.entity.vo.sms.SmsTemplateVO;
import com.nz.admin.modules.system.service.sms.SmsService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.*;

/** 短信渠道、模板和发送记录接口。 */
@RestController
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequestMapping("/api/system/sms")
@ConditionalOnBean(SmsGateway.class)
public class SmsController {
    private final SmsService smsService;

    public SmsController(SmsService smsService) {
        this.smsService = smsService;
    }

    @SaCheckPermission("system:sms:list")
    @GetMapping("/channels/page")
    public R<IPage<SmsChannelVO>> pageChannels(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer status) {
        return R.ok(smsService.pageChannels(pageNum, pageSize, keyword, status));
    }

    @SaCheckPermission("system:sms:query")
    @GetMapping("/channels/{id}")
    public R<SmsChannelVO> getChannel(@PathVariable Long id) {
        return R.ok(smsService.getChannel(id));
    }

    @Log(title = "短信渠道", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:sms:add")
    @PostMapping("/channels")
    public R<Long> createChannel(@Valid @RequestBody SmsChannelSaveRequest request) {
        return R.ok(smsService.createChannel(request));
    }

    @Log(title = "短信渠道", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:sms:edit")
    @PutMapping("/channels")
    public R<Void> updateChannel(@Valid @RequestBody SmsChannelSaveRequest request) {
        smsService.updateChannel(request);
        return R.ok();
    }

    @Log(title = "短信渠道", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:sms:remove")
    @DeleteMapping("/channels/{id}")
    public R<Void> deleteChannel(@PathVariable Long id) {
        smsService.deleteChannel(id);
        return R.ok();
    }

    @SaCheckPermission("system:sms:list")
    @GetMapping("/templates/page")
    public R<IPage<SmsTemplateVO>> pageTemplates(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) Long channelId,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer status) {
        return R.ok(smsService.pageTemplates(pageNum, pageSize, channelId, keyword, status));
    }

    @SaCheckPermission("system:sms:query")
    @GetMapping("/templates/{id}")
    public R<SmsTemplateVO> getTemplate(@PathVariable Long id) {
        return R.ok(smsService.getTemplate(id));
    }

    @Log(title = "短信模板", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:sms:add")
    @PostMapping("/templates")
    public R<Long> createTemplate(@Valid @RequestBody SmsTemplateSaveRequest request) {
        return R.ok(smsService.createTemplate(request));
    }

    @Log(title = "短信模板", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:sms:edit")
    @PutMapping("/templates")
    public R<Void> updateTemplate(@Valid @RequestBody SmsTemplateSaveRequest request) {
        smsService.updateTemplate(request);
        return R.ok();
    }

    @Log(title = "短信模板", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:sms:remove")
    @DeleteMapping("/templates/{id}")
    public R<Void> deleteTemplate(@PathVariable Long id) {
        smsService.deleteTemplate(id);
        return R.ok();
    }

    @SaCheckPermission("system:sms:list")
    @GetMapping("/logs/page")
    public R<IPage<SmsSendLogVO>> pageLogs(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           @RequestParam(required = false) String sendStatus) {
        return R.ok(smsService.pageLogs(pageNum, pageSize, sendStatus));
    }

    @Log(title = "短信测试发送", businessType = BusinessType.OTHER)
    @SaCheckPermission("system:sms:send")
    @PostMapping("/send-test")
    public R<Long> sendTest(@Valid @RequestBody SmsTestSendRequest request) {
        return R.ok(smsService.sendTest(request));
    }
}
