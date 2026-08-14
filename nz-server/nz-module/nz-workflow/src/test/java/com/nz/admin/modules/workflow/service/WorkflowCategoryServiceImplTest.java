package com.nz.admin.modules.workflow.service;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowCategoryDO;
import com.nz.admin.modules.workflow.entity.dto.WorkflowCategoryCreateRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowCategoryUpdateRequest;
import com.nz.admin.modules.workflow.mapper.WorkflowCategoryMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 流程分类服务测试。
 */
class WorkflowCategoryServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private WorkflowCategoryMapper categoryMapper;

    @Mock
    private WorkflowDefinitionReferenceChecker definitionReferenceChecker;

    private WorkflowCategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new WorkflowCategoryServiceImpl(definitionReferenceChecker);
        ReflectionTestUtils.setField(categoryService, "baseMapper", categoryMapper);
    }

    @Test
    void createChildShouldCalculateAncestorsAndTrimName() {
        WorkflowCategoryDO parent = category(3L, 1L, "0,1", "人事审批", 0);
        when(categoryMapper.selectCount(any())).thenReturn(0L);
        when(categoryMapper.selectById(3L)).thenReturn(parent);
        when(categoryMapper.insert(any(WorkflowCategoryDO.class))).thenAnswer(invocation -> {
            WorkflowCategoryDO category = invocation.getArgument(0);
            category.setCategoryId(8L);
            return 1;
        });

        WorkflowCategoryCreateRequest request = new WorkflowCategoryCreateRequest();
        request.setParentId(3L);
        request.setCategoryName("  请假  ");
        request.setOrderNum(10);

        assertThat(categoryService.create(request)).isEqualTo(8L);
        ArgumentCaptor<WorkflowCategoryDO> captor = ArgumentCaptor.forClass(WorkflowCategoryDO.class);
        verify(categoryMapper).insert(captor.capture());
        assertThat(captor.getValue().getAncestors()).isEqualTo("0,1,3");
        assertThat(captor.getValue().getCategoryName()).isEqualTo("请假");
        assertThat(captor.getValue().getBuiltIn()).isZero();
    }

    @Test
    void updateShouldRejectMovingCategoryUnderItsDescendant() {
        WorkflowCategoryDO current = category(3L, 1L, "0,1", "人事审批", 0);
        WorkflowCategoryDO descendant = category(8L, 3L, "0,1,3", "请假", 0);
        when(categoryMapper.selectById(3L)).thenReturn(current);
        when(categoryMapper.selectById(8L)).thenReturn(descendant);
        when(categoryMapper.selectCount(any())).thenReturn(0L);

        WorkflowCategoryUpdateRequest request = new WorkflowCategoryUpdateRequest();
        request.setCategoryId(3L);
        request.setParentId(8L);
        request.setCategoryName("人事审批");
        request.setOrderNum(1);

        assertThrows(BusinessException.class, () -> categoryService.update(request));
        verify(categoryMapper, never()).updateById(any(WorkflowCategoryDO.class));
    }

    @Test
    void deleteShouldRejectBuiltInCategory() {
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, 0L, "0", "OA审批", 1));

        assertThrows(BusinessException.class, () -> categoryService.delete(1L));
        verify(categoryMapper, never()).deleteById(1L);
    }

    @Test
    void deleteShouldRejectReferencedCategory() {
        when(categoryMapper.selectById(9L)).thenReturn(category(9L, 1L, "0,1", "报销", 0));
        when(categoryMapper.selectCount(any())).thenReturn(0L);
        when(definitionReferenceChecker.hasDefinitions(9L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> categoryService.delete(9L));
        verify(categoryMapper, never()).deleteById(9L);
    }

    @Test
    void treeShouldAttachChildrenToTheirParent() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, 0L, "0", "OA审批", 1),
                category(3L, 1L, "0,1", "人事审批", 0)));

        assertThat(categoryService.tree(null)).singleElement().satisfies(root -> {
            assertThat(root.getCategoryName()).isEqualTo("OA审批");
            assertThat(root.getChildren()).singleElement()
                    .extracting("categoryName")
                    .isEqualTo("人事审批");
        });
    }

    private WorkflowCategoryDO category(Long id, Long parentId, String ancestors, String name, Integer builtIn) {
        return new WorkflowCategoryDO()
                .setCategoryId(id)
                .setParentId(parentId)
                .setAncestors(ancestors)
                .setCategoryName(name)
                .setOrderNum(0)
                .setBuiltIn(builtIn);
    }
}
