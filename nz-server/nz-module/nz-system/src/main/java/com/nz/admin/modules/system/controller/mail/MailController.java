package com.nz.admin.modules.system.controller.mail;

import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.framework.mail.core.MailMessage;
import com.nz.admin.framework.mail.core.MailService;
import com.nz.admin.modules.system.entity.dto.mail.MailTestRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/mail")
@ConditionalOnBean(MailService.class)
public class MailController {

    private final MailService mailService;

    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    @PostMapping("/test")
    @SaCheckPermission("system:mail:test")
    @Log(title = "Mail test", businessType = BusinessType.OTHER)
    public R<Void> sendTest(@Valid @RequestBody MailTestRequest request) {
        mailService.send(new MailMessage(request.getTo(), request.getSubject(), request.getContent(), request.isHtml()));
        return R.ok();
    }
}