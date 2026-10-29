package com.nz.admin.modules.job.service.job;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.job.JobTenantProvider;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.job.entity.dataobject.job.JobDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobSchedulerInitializerTest {
    @Mock JobService jobService;
    @Mock JobTenantProvider tenantProvider;
    @InjectMocks JobSchedulerInitializer initializer;

    @Test
    void loadsEveryPageForEnabledTenantsAndRestoresTenantContext() {
        when(tenantProvider.enabledTenantIds()).thenReturn(List.of(11L, 22L));
        when(jobService.listPage(anyInt(), anyInt(), isNull(), isNull(), isNull())).thenAnswer(invocation -> {
            int pageNumber = invocation.getArgument(0);
            Long tenantId = TenantContextHolder.getTenantIdOrNull();
            Page<JobDO> page = new Page<>(pageNumber, 200);
            page.setRecords(pageNumber == 1 ? java.util.stream.IntStream.range(0, 200)
                    .mapToObj(i -> new JobDO().setId((tenantId * 1000) + i).setTenantId(tenantId)).toList()
                    : List.of(new JobDO().setId(tenantId * 1000 + 999).setTenantId(tenantId)));
            return page;
        });
        TenantContextHolder.setTenantId(99L);
        try {
            initializer.run(null);
            var captor = org.mockito.ArgumentCaptor.forClass(List.class);
            verify(jobService).initializeScheduler(captor.capture());
            assertThat(captor.getValue()).hasSize(402);
            assertThat(TenantContextHolder.getTenantIdOrNull()).isEqualTo(99L);
        } finally {
            TenantContextHolder.clear();
        }
        verify(jobService, times(2)).listPage(1, 200, null, null, null);
        verify(jobService, times(2)).listPage(2, 200, null, null, null);
    }
}
