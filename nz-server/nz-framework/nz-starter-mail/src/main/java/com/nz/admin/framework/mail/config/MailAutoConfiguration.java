package com.nz.admin.framework.mail.config;

import com.nz.admin.framework.mail.core.JavaMailService;
import com.nz.admin.framework.mail.core.MailService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.StringUtils;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration")
@ConditionalOnBean(JavaMailSender.class)
@ConditionalOnProperty(prefix = "nz.mail", name = "enabled", havingValue = "true")
public class MailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MailService.class)
    MailService mailService(JavaMailSender mailSender, MailProperties properties) {
        String from = properties.getProperties().get("from");
        if (!StringUtils.hasText(from)) {
            from = properties.getUsername();
        }
        if (!StringUtils.hasText(from)) {
            throw new IllegalStateException("Configure spring.mail.username or spring.mail.properties.from");
        }
        return new JavaMailService(mailSender, from);
    }
}
