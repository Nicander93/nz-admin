package com.nz.admin.framework.mail.core;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/** JavaMail-backed implementation kept behind the starter's conditional configuration. */
public class JavaMailService implements MailService {

    private final JavaMailSender mailSender;
    private final String from;

    public JavaMailService(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(MailMessage message) {
        var mimeMessage = mailSender.createMimeMessage();
        try {
            var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.content(), message.html());
            mailSender.send(mimeMessage);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create outbound email", exception);
        }
    }
}
