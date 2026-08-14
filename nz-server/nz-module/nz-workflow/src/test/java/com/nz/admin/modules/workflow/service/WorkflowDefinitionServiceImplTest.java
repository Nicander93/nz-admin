package com.nz.admin.modules.workflow.service;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowCategoryDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowDefinitionDO;
import com.nz.admin.modules.workflow.entity.dto.WorkflowDefinitionCreateRequest;
import com.nz.admin.modules.workflow.mapper.WorkflowCategoryMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowDefinitionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 流程定义服务测试。
 */
class WorkflowDefinitionServiceImplTest extends BaseMockitoUnitTest {

    private static final String MODEL_JSON = """
            {"nodes":[
              {"id":"start","type":"start"},
              {"id":"approve","type":"task","assignee":"role:manager"},
              {"id":"end","type":"end"}
            ],"edges":[
              {"source":"start","target":"approve"},
              {"source":"approve","target":"end"}
            ]}
            """;

    @Mock
    private WorkflowDefinitionMapper definitionMapper;

    @Mock
    private WorkflowCategoryMapper categoryMapper;

    @Mock
    private WorkflowDefinitionUsageChecker usageChecker;

    private WorkflowDefinitionServiceImpl definitionService;

    @BeforeEach
    void setUp() {
        definitionService = new WorkflowDefinitionServiceImpl(
                categoryMapper, usageChecker, new WorkflowModelValidator());
        ReflectionTestUtils.setField(definitionService, "baseMapper", definitionMapper);
    }

    @Test
    void createShouldAppendVersionAfterPublishedDefinition() {
        when(categoryMapper.selectById(3L)).thenReturn(new WorkflowCategoryDO()
                .setCategoryId(3L)
                .setCategoryName("人事审批"));
        when(definitionMapper.selectList(any())).thenReturn(List.of(definition(7L, 1, 1)));
        when(definitionMapper.insert(any(WorkflowDefinitionDO.class))).thenAnswer(invocation -> {
            WorkflowDefinitionDO definition = invocation.getArgument(0);
            definition.setDefinitionId(8L);
            return 1;
        });

        Long id = definitionService.create(createRequest());

        ArgumentCaptor<WorkflowDefinitionDO> captor = ArgumentCaptor.forClass(WorkflowDefinitionDO.class);
        verify(definitionMapper).insert(captor.capture());
        assertThat(id).isEqualTo(8L);
        assertThat(captor.getValue().getVersionNo()).isEqualTo(2);
        assertThat(captor.getValue().getPublishStatus()).isZero();
    }

    @Test
    void createShouldRejectSecondDraftForSameCode() {
        when(categoryMapper.selectById(3L)).thenReturn(new WorkflowCategoryDO()
                .setCategoryId(3L)
                .setCategoryName("人事审批"));
        when(definitionMapper.selectList(any())).thenReturn(List.of(definition(8L, 2, 0)));

        assertThrows(BusinessException.class, () -> definitionService.create(createRequest()));
        verify(definitionMapper, never()).insert(any(WorkflowDefinitionDO.class));
    }

    @Test
    void publishShouldExpirePreviousPublishedVersion() {
        WorkflowDefinitionDO draft = definition(8L, 2, 0);
        WorkflowDefinitionDO previous = definition(7L, 1, 1);
        when(definitionMapper.selectById(8L)).thenReturn(draft);
        when(definitionMapper.selectList(any())).thenReturn(List.of(previous));

        definitionService.publish(8L);

        ArgumentCaptor<WorkflowDefinitionDO> captor = ArgumentCaptor.forClass(WorkflowDefinitionDO.class);
        verify(definitionMapper, times(2)).updateById(captor.capture());
        assertThat(previous.getPublishStatus()).isEqualTo(9);
        assertThat(previous.getActivityStatus()).isZero();
        assertThat(draft.getPublishStatus()).isEqualTo(1);
        assertThat(draft.getActivityStatus()).isEqualTo(1);
    }

    @Test
    void unpublishShouldRejectDefinitionUsedByInstance() {
        WorkflowDefinitionDO published = definition(8L, 2, 1);
        when(definitionMapper.selectById(8L)).thenReturn(published);
        when(usageChecker.isUsed(8L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> definitionService.unpublish(8L));
        verify(definitionMapper, never()).updateById(any(WorkflowDefinitionDO.class));
    }

    @Test
    void unpublishShouldRejectWhenAnotherDraftExists() {
        WorkflowDefinitionDO published = definition(7L, 1, 1);
        when(definitionMapper.selectById(7L)).thenReturn(published);
        when(definitionMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> definitionService.unpublish(7L));
        verify(definitionMapper, never()).updateById(any(WorkflowDefinitionDO.class));
    }

    private WorkflowDefinitionCreateRequest createRequest() {
        WorkflowDefinitionCreateRequest request = new WorkflowDefinitionCreateRequest();
        request.setFlowCode("leave_apply");
        request.setFlowName("请假审批");
        request.setCategoryId(3L);
        request.setModelJson(MODEL_JSON);
        return request;
    }

    private WorkflowDefinitionDO definition(Long id, Integer version, Integer status) {
        return new WorkflowDefinitionDO()
                .setDefinitionId(id)
                .setFlowCode("leave_apply")
                .setFlowName("请假审批")
                .setCategoryId(3L)
                .setVersionNo(version)
                .setPublishStatus(status)
                .setActivityStatus(1)
                .setModelJson(MODEL_JSON);
    }
}
