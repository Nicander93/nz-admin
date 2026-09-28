package com.nz.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nz.admin.modules.system")
@MapperScan("com.nz.admin.modules.system.mapper")
public class NzSystemTestApplication {
}
