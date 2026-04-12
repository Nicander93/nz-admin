package com.nz.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nz.admin.modules.system.mapper")
public class NzAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(NzAdminApplication.class, args);
    }
}
