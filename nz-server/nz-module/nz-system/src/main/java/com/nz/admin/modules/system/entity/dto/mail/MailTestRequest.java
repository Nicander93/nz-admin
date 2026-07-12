package com.nz.admin.modules.system.entity.dto.mail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MailTestRequest {
    @Email
    @NotBlank
    private String to;
    @NotBlank
    private String subject;
    @NotBlank
    private String content;
    private boolean html;
}