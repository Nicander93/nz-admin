package com.nz.admin.framework.mail.core;

/** Application-facing email sending port. */
public interface MailService {

    void send(MailMessage message);
}
