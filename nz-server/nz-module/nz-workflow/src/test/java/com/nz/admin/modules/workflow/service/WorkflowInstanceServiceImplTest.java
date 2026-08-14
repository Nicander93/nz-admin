package com.nz.admin.modules.workflow.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.nz.admin.common.module.NzUserNotificationPublisher;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.auth.core.LoginUser;
import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowDefinitionDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowInstanceDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowInstanceEventDO;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceActionRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceStartRequest;
import com.nz.admin.modules.workflow.mapper.WorkflowDefinitionMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowInstanceEventMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowInstanceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 流程实例服务测试。 */
class WorkflowInstanceServiceImplTest extends BaseMockitoUnitTest {

    private static final String MODEL_JSON = """
            {"nodes":[
              {"id":"start","type":"start","name":"开始"},
              {"id":"approve","type":"task","name":"审批","assignee":"role:manager"},
              {"id":"end","type":"end","name":"结束"}
            ],"edges":[
              {"source":"start","target":"approve"},
              {"source":"approve","target":"end"}
            ]}
            """;

    @Mock
    private WorkflowInstanceMapper instanceMapper;
    @Mock
    private WorkflowInstanceEventMapper eventMapper;
    @Mock
    private WorkflowDefinitionMapper definitionMapper;
    @Mock
    private LoginUserContext loginUserContext;
    @Mock
    private WorkflowTaskLifecycleService taskLifecycleService;
    @Mock
    private NzUserNotificationPublisher notificationPublisher;

    private WorkflowInstanceServiceImpl instanceService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WorkflowInstanceDO.class);
        instanceService = new WorkflowInstanceServiceImpl(
                definitionMapper, eventMapper, loginUserContext, new WorkflowRuntimeResolver(),
                taskLifecycleService, java.util.Optional.of(notificationPublisher));
        ReflectionTestUtils.setField(instanceService, "baseMapper", instanceMapper);
    }

    @Test
    void startShouldSnapshotPublishedDefinitionAndCreateFirstTaskEvent() {
        when(loginUserContext.getLoginUserOrNull()).thenReturn(login(9L, Set.of("employee")));
        when(definitionMapper.selectOne(any())).thenReturn(publishedDefinition());
        when(instanceMapper.insert(any(WorkflowInstanceDO.class))).thenAnswer(invocation -> {
            WorkflowInstanceDO instance = invocation.getArgument(0);
            instance.setInstanceId(21L);
            return 1;
        });

        WorkflowInstanceStartRequest request = new WorkflowInstanceStartRequest();
        request.setFlowCode("leave_apply");
        request.setBusinessKey("LEAVE-2026-001");
        request.setTitle("张三请假");
        request.setVariables(Map.of("days", 2));

        Long id = instanceService.start(request);

        ArgumentCaptor<WorkflowInstanceDO> instanceCaptor = ArgumentCaptor.forClass(WorkflowInstanceDO.class);
        ArgumentCaptor<WorkflowInstanceEventDO> eventCaptor = ArgumentCaptor.forClass(WorkflowInstanceEventDO.class);
        verify(instanceMapper).insert(instanceCaptor.capture());
        verify(eventMapper).insert(eventCaptor.capture());
        assertThat(id).isEqualTo(21L);
        assertThat(instanceCaptor.getValue().getModelJson()).isEqualTo(MODEL_JSON);
        assertThat(instanceCaptor.getValue().getCurrentNodeId()).isEqualTo("approve");
        assertThat(instanceCaptor.getValue().getCurrentAssignee()).isEqualTo("role:manager");
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("START");
    }

    @Test
    void actionShouldRejectUserWhoIsNotCurrentAssignee() {
        WorkflowInstanceDO instance = runningInstance();
        when(loginUserContext.getLoginUserOrNull()).thenReturn(login(9L, Set.of("employee")));
        when(instanceMapper.selectById(21L)).thenReturn(instance);

        WorkflowInstanceActionRequest request = new WorkflowInstanceActionRequest();
        request.setAction("APPROVE");

        assertThrows(BusinessException.class, () -> instanceService.action(21L, request));
        verify(instanceMapper, never()).update(
                ArgumentMatchers.<WorkflowInstanceDO>isNull(),
                ArgumentMatchers.<Wrapper<WorkflowInstanceDO>>any());
    }

    @Test
    void approveShouldCompleteSequentialInstanceAndAppendEvent() {
        WorkflowInstanceDO instance = runningInstance();
        when(loginUserContext.getLoginUserOrNull()).thenReturn(login(7L, Set.of("manager")));
        when(instanceMapper.selectById(21L)).thenReturn(instance);
        when(instanceMapper.update(
                ArgumentMatchers.<WorkflowInstanceDO>isNull(),
                ArgumentMatchers.<Wrapper<WorkflowInstanceDO>>any())).thenReturn(1);

        WorkflowInstanceActionRequest request = new WorkflowInstanceActionRequest();
        request.setAction("APPROVE");
        request.setComment("同意");

        instanceService.action(21L, request);

        ArgumentCaptor<WorkflowInstanceEventDO> eventCaptor = ArgumentCaptor.forClass(WorkflowInstanceEventDO.class);
        verify(eventMapper).insert(eventCaptor.capture());
        assertThat(instance.getStatus()).isEqualTo("COMPLETED");
        assertThat(instance.getCurrentNodeId()).isEqualTo("end");
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("APPROVE");
        assertThat(eventCaptor.getValue().getComment()).isEqualTo("同意");
    }

    private WorkflowDefinitionDO publishedDefinition() {
        return new WorkflowDefinitionDO()
                .setDefinitionId(8L)
                .setFlowCode("leave_apply")
                .setFlowName("请假审批")
                .setVersionNo(2)
                .setPublishStatus(1)
                .setActivityStatus(1)
                .setModelJson(MODEL_JSON);
    }

    private WorkflowInstanceDO runningInstance() {
        return new WorkflowInstanceDO()
                .setInstanceId(21L)
                .setDefinitionId(8L)
                .setBusinessKey("LEAVE-2026-001")
                .setTitle("张三请假")
                .setFlowCode("leave_apply")
                .setFlowName("请假审批")
                .setVersionNo(2)
                .setInitiatorId(9L)
                .setCurrentNodeId("approve")
                .setCurrentNodeName("审批")
                .setCurrentNodeType("task")
                .setCurrentAssignee("role:manager")
                .setStatus("RUNNING")
                .setActivityStatus(1)
                .setVariablesJson("{}")
                .setModelJson(MODEL_JSON);
    }

    private LoginUser login(Long userId, Set<String> roles) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setUsername("user-" + userId);
        loginUser.setRoles(roles);
        return loginUser;
    }
}
