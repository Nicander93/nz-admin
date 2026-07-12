package com.nz.admin.framework.mail.config;

import com.nz.admin.framework.mail.core.MailMessage;
import com.nz.admin.framework.mail.core.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MailAutoConfiguration.class))
            .withUserConfiguration(JavaMailTestConfiguration.class)
            .withPropertyValues("nz.mail.enabled=true", "spring.mail.username=no-reply@example.com");

    @Test
    void createsMailServiceWhenEnabledAndSenderExists() {
        runner.run(context -> assertThat(context).hasSingleBean(MailService.class));
    }

    @Test
    void staysDisabledByDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MailAutoConfiguration.class))
                .withUserConfiguration(JavaMailTestConfiguration.class)
                .run(context -> assertThat(context).doesNotHaveBean(MailService.class));
    }

    @Test
    void sendsHtmlMessageThroughJavaMailSender() {
        runner.run(context -> {
            JavaMailSender sender = context.getBean(JavaMailSender.class);
            var mime = new MimeMessage(Session.getInstance(new Properties()));
            when(sender.createMimeMessage()).thenReturn(mime);
            context.getBean(MailService.class).send(new MailMessage("user@example.com", "Subject", "<b>Hello</b>", true));
            verify(sender).send(mime);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class JavaMailTestConfiguration {
        @Bean JavaMailSender javaMailSender() { return mock(JavaMailSender.class); }
        @Bean MailProperties mailProperties() {
            MailProperties properties = new MailProperties();
            properties.setUsername("no-reply@example.com");
            return properties;
        }
    }
}
