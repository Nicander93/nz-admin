package com.nz.admin.modules.job.service.job;

import com.nz.admin.common.job.JobTenantProvider;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.job.entity.dataobject.job.JobDO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/** 每次启动恢复调度，独立于是否初始化演示/管理员数据。 */
@Component
@Order(100)
@RequiredArgsConstructor
public class JobSchedulerInitializer implements ApplicationRunner {
    private final JobService jobService;
    private final JobTenantProvider tenantProvider;

    @Override
    public void run(ApplicationArguments args) {
        var jobs = new ArrayList<JobDO>();
        for (Long tenantId : tenantProvider.enabledTenantIds()) {
            int pageNumber = 1;
            while (true) {
                int page = pageNumber++;
                var records = TenantContextHolder.callWithTenantId(tenantId,
                        () -> jobService.listPage(page, 200, null, null, 0).getRecords());
                jobs.addAll(records);
                if (records.size() < 200) break;
            }
        }
        jobService.initializeScheduler(jobs);
    }
}
