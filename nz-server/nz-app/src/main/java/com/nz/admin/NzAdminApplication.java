package com.nz.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = QuartzAutoConfiguration.class)
@EnableAsync
@MapperScan("com.nz.admin.modules.system.mapper")
public class NzAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(NzAdminApplication.class, args);
    }
}
