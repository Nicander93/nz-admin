package com.nz.admin;

import com.nz.admin.common.job.JobExecuteService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = "com.nz.admin.modules.system")
@MapperScan("com.nz.admin.modules.system.mapper")
@Import(JobExecuteService.class)
public class NzSystemTestApplication {
}
