package com.nz.admin.modules.workflow.service;

import com.nz.admin.framework.auth.core.LoginUser;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowHistoryTaskDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowInstanceDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowTaskDO;
import com.nz.admin.modules.workflow.mapper.WorkflowHistoryTaskMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowTaskCopyMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 任务生命周期测试。 */
class WorkflowTaskLifecycleServiceTest extends BaseMockitoUnitTest {

    @Mock
    private WorkflowTaskMapper taskMapper;
    @Mock
    private WorkflowHistoryTaskMapper historyTaskMapper;
    @Mock
    private WorkflowTaskCopyMapper taskCopyMapper;

    private WorkflowTaskLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        lifecycleService = new WorkflowTaskLifecycleService(taskMapper, historyTaskMapper, taskCopyMapper);
    }

    @Test
    void createCurrentShouldResolveInitiatorAsConcreteUser() {
        WorkflowInstanceDO instance = new WorkflowInstanceDO()
                .setInstanceId(21L)
                .setDefinitionId(8L)
                .setInitiatorId(9L)
                .setCurrentNodeId("approve")
                .setCurrentNodeName("审批")
                .setCurrentAssignee("initiator")
                .setStatus("RUNNING");

        lifecycleService.createCurrent(instance);

        ArgumentCaptor<WorkflowTaskDO> captor = ArgumentCaptor.forClass(WorkflowTaskDO.class);
        verify(taskMapper).insert(captor.capture());
        assertThat(captor.getValue().getAssigneeUserId()).isEqualTo(9L);
        assertThat(captor.getValue().getInstanceId()).isEqualTo(21L);
    }

    @Test
    void archiveCurrentShouldWriteHistoryAndRemoveTodo() {
        WorkflowTaskDO task = new WorkflowTaskDO()
                .setTaskId(31L)
                .setDefinitionId(8L)
                .setInstanceId(21L)
                .setNodeId("approve")
                .setNodeName("审批")
                .setAssignee("role:manager");
        task.setCreateTime(LocalDateTime.now().minusHours(1));
        when(taskMapper.selectOne(any())).thenReturn(task);
        when(taskMapper.deleteById(31L)).thenReturn(1);

        LoginUser operator = new LoginUser();
        operator.setUserId(7L);
        operator.setUsername("manager");
        lifecycleService.archiveCurrent(21L, "approve", operator, "APPROVE",
                new WorkflowRuntimeResolver.RuntimeNode("end", "结束", "end", null), "同意");

        ArgumentCaptor<WorkflowHistoryTaskDO> captor = ArgumentCaptor.forClass(WorkflowHistoryTaskDO.class);
        verify(historyTaskMapper).insert(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("APPROVE");
        assertThat(captor.getValue().getTargetNodeId()).isEqualTo("end");
        assertThat(captor.getValue().getComment()).isEqualTo("同意");
        verify(taskMapper).deleteById(31L);
    }
    @Test
    void requireCompletableShouldRejectDelegatedTask() {
        WorkflowTaskDO task = new WorkflowTaskDO()
                .setTaskId(31L)
                .setInstanceId(21L)
                .setNodeId("approve")
                .setDelegationStatus(1);
        when(taskMapper.selectOne(any())).thenReturn(task);

        assertThrows(BusinessException.class,
                () -> lifecycleService.requireCompletable(21L, "approve"));
    }

}
