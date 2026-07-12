package com.nz.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(
        scanBasePackages = {"com.nz.admin.config", "com.nz.admin.controller"},
        exclude = QuartzAutoConfiguration.class
)
@EnableAsync
public class NzAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(NzAdminApplication.class, args);
    }
}
