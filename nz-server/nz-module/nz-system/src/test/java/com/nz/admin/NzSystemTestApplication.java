package com.nz.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nz.admin.modules.system")
@MapperScan("com.nz.admin.modules.system.mapper")
public class NzSystemTestApplication {
    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    com.nz.admin.framework.datascope.core.DataScopeResolver unitTestDataScopeResolver() {
        return () -> new com.nz.admin.framework.datascope.core.DataScopeResult(true, java.util.Set.of(), false, 1L);
    }

}
