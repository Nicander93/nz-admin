package com.nz.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.nz.admin.common.job.JobExecuteService;
import com.nz.admin.common.module.NzWorkflowAssigneeResolver;
import com.nz.admin.modules.workflow.handler.NzWarmFlowDataFillHandler;
import com.nz.admin.modules.workflow.handler.NzWarmFlowPermissionHandler;
import com.nz.admin.modules.workflow.handler.NzWarmFlowTenantHandler;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.config.WarmFlow;
import org.dromara.warm.flow.core.service.DefService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = NzAdminApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("monolith-test")
class NzAdminApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void startsProductionApplicationWithH2AndReportsReady() {
        assertThat(applicationContext.getBean(JobExecuteService.class)).isNotNull();
        assertThat(applicationContext.getBean(NzWorkflowAssigneeResolver.class)).isNotNull();
        assertThat(applicationContext.getBean("initFlow", WarmFlow.class)).isNotNull();
        assertThat(applicationContext.getBean(DefService.class)).isNotNull();
        assertThat(FlowEngine.dataFillHandler()).isInstanceOf(NzWarmFlowDataFillHandler.class);
        assertThat(FlowEngine.permissionHandler()).isInstanceOf(NzWarmFlowPermissionHandler.class);
        assertThat(FlowEngine.tenantHandler()).isInstanceOf(NzWarmFlowTenantHandler.class);

        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/actuator/health/readiness", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("status").asText()).isEqualTo("UP");
    }
}
