package com.nz.admin.modules.workflow.service;

import com.nz.admin.common.core.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 流程模型校验测试。
 */
class WorkflowModelValidatorTest {

    private final WorkflowModelValidator validator = new WorkflowModelValidator();

    @Test
    void acceptsConnectedApprovalModel() {
        String normalized = validator.normalizeAndValidate("""
                {"nodes":[
                  {"id":"start","type":"start"},
                  {"id":"approve","type":"task","assignee":"role:manager"},
                  {"id":"end","type":"end"}
                ],"edges":[
                  {"source":"start","target":"approve"},
                  {"source":"approve","target":"end"}
                ]}
                """);

        assertThat(normalized).contains("role:manager", "\"source\":\"start\"");
    }

    @Test
    void rejectsTaskWithoutAssignee() {
        assertThrows(BusinessException.class, () -> validator.normalizeAndValidate("""
                {"nodes":[
                  {"id":"start","type":"start"},
                  {"id":"approve","type":"task"},
                  {"id":"end","type":"end"}
                ],"edges":[
                  {"source":"start","target":"approve"},
                  {"source":"approve","target":"end"}
                ]}
                """));
    }

    @Test
    void rejectsOrphanNode() {
        assertThrows(BusinessException.class, () -> validator.normalizeAndValidate("""
                {"nodes":[
                  {"id":"start","type":"start"},
                  {"id":"approve","type":"task","assignee":"user:1"},
                  {"id":"end","type":"end"}
                ],"edges":[{"source":"start","target":"end"}]}
                """));
    }
    @Test
    void rejectsUnsupportedAssigneeExpression() {
        assertThrows(BusinessException.class, () -> validator.normalizeAndValidate("""
                {"nodes":[
                  {"id":"start","type":"start"},
                  {"id":"approve","type":"task","assignee":"team:manager"},
                  {"id":"end","type":"end"}
                ],"edges":[
                  {"source":"start","target":"approve"},
                  {"source":"approve","target":"end"}
                ]}
                """));
    }
}
