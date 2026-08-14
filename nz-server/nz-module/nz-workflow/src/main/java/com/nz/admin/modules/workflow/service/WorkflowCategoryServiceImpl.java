package com.nz.admin.modules.workflow.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.modules.workflow.convert.WorkflowCategoryConvert;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowCategoryDO;
import com.nz.admin.modules.workflow.entity.dto.WorkflowCategoryCreateRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowCategoryUpdateRequest;
import com.nz.admin.modules.workflow.entity.vo.WorkflowCategoryVO;
import com.nz.admin.modules.workflow.mapper.WorkflowCategoryMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 流程分类服务实现。
 */
@Service
public class WorkflowCategoryServiceImpl extends ServiceImpl<WorkflowCategoryMapper, WorkflowCategoryDO>
        implements WorkflowCategoryService {

    private static final Long ROOT_ID = 0L;

    private final WorkflowDefinitionReferenceChecker definitionReferenceChecker;

    public WorkflowCategoryServiceImpl(WorkflowDefinitionReferenceChecker definitionReferenceChecker) {
        this.definitionReferenceChecker = definitionReferenceChecker;
    }

    @Override
    public List<WorkflowCategoryDO> list(String categoryName, Long parentId) {
        return baseMapper.selectList(new LambdaQueryWrapper<WorkflowCategoryDO>()
                .like(StrUtil.isNotBlank(categoryName), WorkflowCategoryDO::getCategoryName, StrUtil.trim(categoryName))
                .eq(parentId != null, WorkflowCategoryDO::getParentId, parentId)
                .orderByAsc(WorkflowCategoryDO::getAncestors)
                .orderByAsc(WorkflowCategoryDO::getOrderNum)
                .orderByAsc(WorkflowCategoryDO::getCategoryId));
    }

    @Override
    public List<WorkflowCategoryVO> tree(String categoryName) {
        List<WorkflowCategoryDO> categories = list(categoryName, null);
        Map<Long, WorkflowCategoryVO> nodes = new LinkedHashMap<>();
        categories.forEach(category -> nodes.put(category.getCategoryId(), WorkflowCategoryConvert.toVO(category)));

        List<WorkflowCategoryVO> roots = new ArrayList<>();
        for (WorkflowCategoryVO node : nodes.values()) {
            WorkflowCategoryVO parent = nodes.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    @Override
    public WorkflowCategoryDO getRequired(Long categoryId) {
        WorkflowCategoryDO category = baseMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("流程分类不存在");
        }
        return category;
    }

    @Override
    public Long create(WorkflowCategoryCreateRequest request) {
        String categoryName = StrUtil.trim(request.getCategoryName());
        checkNameUnique(request.getParentId(), categoryName, null);

        WorkflowCategoryDO category = new WorkflowCategoryDO()
                .setParentId(request.getParentId())
                .setAncestors(resolveAncestors(request.getParentId(), null))
                .setCategoryName(categoryName)
                .setOrderNum(request.getOrderNum())
                .setBuiltIn(0);
        baseMapper.insert(category);
        return category.getCategoryId();
    }

    @Override
    public void update(WorkflowCategoryUpdateRequest request) {
        WorkflowCategoryDO current = getRequired(request.getCategoryId());
        if (Objects.equals(request.getCategoryId(), request.getParentId())) {
            throw new BusinessException("上级分类不能选择当前分类");
        }
        if (Objects.equals(current.getParentId(), ROOT_ID) && Objects.equals(current.getBuiltIn(), 1)
                && !Objects.equals(request.getParentId(), ROOT_ID)) {
            throw new BusinessException("内置根分类不能移动");
        }

        String categoryName = StrUtil.trim(request.getCategoryName());
        checkNameUnique(request.getParentId(), categoryName, request.getCategoryId());
        String newAncestors = resolveAncestors(request.getParentId(), request.getCategoryId());
        if (!Objects.equals(current.getAncestors(), newAncestors)) {
            updateDescendantAncestors(current, newAncestors);
        }

        current.setParentId(request.getParentId());
        current.setAncestors(newAncestors);
        current.setCategoryName(categoryName);
        current.setOrderNum(request.getOrderNum());
        baseMapper.updateById(current);
    }

    @Override
    public void delete(Long categoryId) {
        WorkflowCategoryDO category = getRequired(categoryId);
        if (Objects.equals(category.getBuiltIn(), 1)) {
            throw new BusinessException("内置流程分类不能删除");
        }
        Long childCount = baseMapper.selectCount(new LambdaQueryWrapper<WorkflowCategoryDO>()
                .eq(WorkflowCategoryDO::getParentId, categoryId));
        if (childCount > 0) {
            throw new BusinessException("存在子分类，不能删除");
        }
        if (definitionReferenceChecker.hasDefinitions(categoryId)) {
            throw new BusinessException("分类已被流程定义使用，不能删除");
        }
        baseMapper.deleteById(categoryId);
    }

    private String resolveAncestors(Long parentId, Long currentCategoryId) {
        if (Objects.equals(parentId, ROOT_ID)) {
            return "0";
        }
        WorkflowCategoryDO parent = getRequired(parentId);
        if (currentCategoryId != null && containsAncestor(parent.getAncestors(), currentCategoryId)) {
            throw new BusinessException("不能将分类移动到自己的下级");
        }
        return parent.getAncestors() + "," + parent.getCategoryId();
    }

    private boolean containsAncestor(String ancestors, Long categoryId) {
        if (StrUtil.isBlank(ancestors)) {
            return false;
        }
        return Arrays.stream(ancestors.split(","))
                .map(String::trim)
                .anyMatch(String.valueOf(categoryId)::equals);
    }

    private void checkNameUnique(Long parentId, String categoryName, Long excludeCategoryId) {
        LambdaQueryWrapper<WorkflowCategoryDO> wrapper = new LambdaQueryWrapper<WorkflowCategoryDO>()
                .eq(WorkflowCategoryDO::getParentId, parentId)
                .eq(WorkflowCategoryDO::getCategoryName, categoryName)
                .ne(excludeCategoryId != null, WorkflowCategoryDO::getCategoryId, excludeCategoryId);
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("同级流程分类名称已存在");
        }
    }

    private void updateDescendantAncestors(WorkflowCategoryDO current, String newAncestors) {
        String oldPrefix = current.getAncestors() + "," + current.getCategoryId();
        String newPrefix = newAncestors + "," + current.getCategoryId();
        for (WorkflowCategoryDO descendant : baseMapper.selectList(null)) {
            String ancestors = descendant.getAncestors();
            if (ancestors != null && (ancestors.equals(oldPrefix) || ancestors.startsWith(oldPrefix + ","))) {
                descendant.setAncestors(newPrefix + ancestors.substring(oldPrefix.length()));
                baseMapper.updateById(descendant);
            }
        }
    }
}
