package com.nz.admin.framework.mail.core;

/** A transport-neutral outbound email. */
public record MailMessage(String to, String subject, String content, boolean html) {
}
