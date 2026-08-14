package com.nz.admin.modules.workflow.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.auth.core.LoginUser;
import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowInstanceDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowTaskDO;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceActionRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowTaskDelegateRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowTaskTransferRequest;
import com.nz.admin.modules.workflow.mapper.WorkflowHistoryTaskMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowInstanceMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowTaskCopyMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowTaskMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 流程任务服务测试。 */
class WorkflowTaskServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private WorkflowTaskMapper taskMapper;
    @Mock
    private WorkflowHistoryTaskMapper historyTaskMapper;
    @Mock
    private WorkflowTaskCopyMapper taskCopyMapper;
    @Mock
    private WorkflowInstanceMapper instanceMapper;
    @Mock
    private WorkflowInstanceService instanceService;
    @Mock
    private WorkflowTaskLifecycleService lifecycleService;
    @Mock
    private LoginUserContext loginUserContext;

    private WorkflowTaskServiceImpl taskService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WorkflowInstanceDO.class);
        taskService = new WorkflowTaskServiceImpl(historyTaskMapper, taskCopyMapper, instanceMapper,
                instanceService, lifecycleService, loginUserContext);
        ReflectionTestUtils.setField(taskService, "baseMapper", taskMapper);
    }

    @Test
    void actionShouldDelegateAuthorizedTaskToInstanceStateMachine() {
        when(loginUserContext.getLoginUserOrNull()).thenReturn(login(7L, Set.of("manager")));
        when(taskMapper.selectById(31L)).thenReturn(task("role:manager", null));

        WorkflowInstanceActionRequest request = new WorkflowInstanceActionRequest();
        request.setAction("APPROVE");
        taskService.action(31L, request);

        verify(instanceService).action(21L, request);
    }

    @Test
    void actionShouldRejectUnassignedUser() {
        when(loginUserContext.getLoginUserOrNull()).thenReturn(login(9L, Set.of("employee")));
        when(taskMapper.selectById(31L)).thenReturn(task("role:manager", null));

        WorkflowInstanceActionRequest request = new WorkflowInstanceActionRequest();
        request.setAction("APPROVE");

        assertThrows(BusinessException.class, () -> taskService.action(31L, request));
        verify(instanceService, never()).action(21L, request);
    }

    @Test
    void actionShouldRejectDelegatedTaskUntilItReturnsToOwner() {
        WorkflowTaskDO task = task("user:12", 12L).setDelegationStatus(1).setOwnerAssignee("user:7");
        when(loginUserContext.getLoginUserOrNull()).thenReturn(login(12L, Set.of("employee")));
        when(taskMapper.selectById(31L)).thenReturn(task);
        WorkflowInstanceActionRequest request = new WorkflowInstanceActionRequest();
        request.setAction("APPROVE");

        assertThrows(BusinessException.class, () -> taskService.action(31L, request));
        verify(instanceService, never()).action(21L, request);
    }

    @Test
    void transferShouldUpdateInstanceAndRecordTransferHistory() {
        WorkflowTaskDO task = task("user:7", 7L);
        when(loginUserContext.getLoginUserOrNull()).thenReturn(login(7L, Set.of("employee")));
        when(taskMapper.selectById(31L)).thenReturn(task);
        when(instanceMapper.update(
                ArgumentMatchers.<WorkflowInstanceDO>isNull(),
                ArgumentMatchers.<Wrapper<WorkflowInstanceDO>>any())).thenReturn(1);

        WorkflowTaskTransferRequest request = new WorkflowTaskTransferRequest();
        request.setTargetUserId(12L);
        request.setComment("请协助处理");
        taskService.transfer(31L, request);

        verify(lifecycleService).transfer(task, loginUserContext.getLoginUserOrNull(), 12L, "请协助处理");
    }
    @Test
    void delegateShouldUpdateInstanceAndKeepOwnerInLifecycle() {
        WorkflowTaskDO task = task("user:7", 7L);
        LoginUser operator = login(7L, Set.of("employee"));
        when(loginUserContext.getLoginUserOrNull()).thenReturn(operator);
        when(taskMapper.selectById(31L)).thenReturn(task);
        when(instanceMapper.update(
                ArgumentMatchers.<WorkflowInstanceDO>isNull(),
                ArgumentMatchers.<Wrapper<WorkflowInstanceDO>>any())).thenReturn(1);
        WorkflowTaskDelegateRequest request = new WorkflowTaskDelegateRequest();
        request.setTargetUserId(12L);
        request.setComment("委托处理");

        taskService.delegate(31L, request);

        verify(lifecycleService).delegate(task, operator, 12L, "委托处理");
    }

    @Test
    void resolveDelegationShouldReturnTaskToOriginalOwner() {
        WorkflowTaskDO task = task("user:12", 12L)
                .setOwnerAssignee("user:7")
                .setOwnerUserId(7L)
                .setDelegationStatus(1);
        LoginUser operator = login(12L, Set.of("employee"));
        when(loginUserContext.getLoginUserOrNull()).thenReturn(operator);
        when(taskMapper.selectById(31L)).thenReturn(task);
        when(instanceMapper.update(
                ArgumentMatchers.<WorkflowInstanceDO>isNull(),
                ArgumentMatchers.<Wrapper<WorkflowInstanceDO>>any())).thenReturn(1);

        taskService.resolveDelegation(31L, "处理完成");

        verify(lifecycleService).resolveDelegation(task, operator, "处理完成");
    }


    private WorkflowTaskDO task(String assignee, Long assigneeUserId) {
        return new WorkflowTaskDO()
                .setTaskId(31L)
                .setDefinitionId(8L)
                .setInstanceId(21L)
                .setNodeId("approve")
                .setNodeName("审批")
                .setAssignee(assignee)
                .setAssigneeUserId(assigneeUserId)
                .setDelegationStatus(0);
    }

    private LoginUser login(Long id, Set<String> roles) {
        LoginUser user = new LoginUser();
        user.setUserId(id);
        user.setUsername("user-" + id);
        user.setRoles(roles);
        return user;
    }
}
