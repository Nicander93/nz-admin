package com.nz.admin.modules.workflow.service;

import com.nz.admin.common.core.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 流程运行路径解析测试。 */
class WorkflowRuntimeResolverTest {

    private final WorkflowRuntimeResolver resolver = new WorkflowRuntimeResolver();

    @Test
    void shouldRunSequentialModelFromStartToEnd() {
        String model = """
                {"nodes":[
                  {"id":"start","type":"start"},
                  {"id":"approve","type":"task","name":"审批","assignee":"role:manager"},
                  {"id":"end","type":"end"}
                ],"edges":[
                  {"source":"start","target":"approve"},
                  {"source":"approve","target":"end"}
                ]}
                """;

        WorkflowRuntimeResolver.Transition started = resolver.start(model, Map.of());
        WorkflowRuntimeResolver.Transition completed = resolver.advance(model, started.target().id(), Map.of());

        assertThat(started.completed()).isFalse();
        assertThat(started.target().assignee()).isEqualTo("role:manager");
        assertThat(completed.completed()).isTrue();
        assertThat(completed.target().type()).isEqualTo("end");
    }

    @Test
    void shouldChooseExclusiveBranchByVariableAndDefault() {
        String model = """
                {"nodes":[
                  {"id":"start","type":"start"},
                  {"id":"route","type":"exclusive"},
                  {"id":"manager","type":"task","assignee":"role:manager"},
                  {"id":"leader","type":"task","assignee":"role:leader"}
                ],"edges":[
                  {"source":"start","target":"route"},
                  {"source":"route","target":"manager","condition":{"variable":"amount","operator":"GT","value":1000}},
                  {"source":"route","target":"leader"}
                ]}
                """;

        assertThat(resolver.start(model, Map.of("amount", 1200)).target().id()).isEqualTo("manager");
        assertThat(resolver.start(model, Map.of("amount", 200)).target().id()).isEqualTo("leader");
    }

    @Test
    void shouldRejectParallelGatewayUntilMultiTaskRuntimeExists() {
        String model = """
                {"nodes":[{"id":"start","type":"start"},{"id":"fork","type":"parallel"}],
                 "edges":[{"source":"start","target":"fork"}]}
                """;

        assertThrows(BusinessException.class, () -> resolver.start(model, Map.of()));
    }
}
