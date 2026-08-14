package com.nz.admin.modules.workflow.service;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.core.PageResult;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowCategoryDO;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowDefinitionDO;
import com.nz.admin.modules.workflow.entity.dto.WorkflowDefinitionCopyRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowDefinitionCreateRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowDefinitionUpdateRequest;
import com.nz.admin.modules.workflow.entity.vo.WorkflowDefinitionVO;
import com.nz.admin.modules.workflow.mapper.WorkflowCategoryMapper;
import com.nz.admin.modules.workflow.mapper.WorkflowDefinitionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程定义服务实现。
 */
@Service
public class WorkflowDefinitionServiceImpl extends ServiceImpl<WorkflowDefinitionMapper, WorkflowDefinitionDO>
        implements WorkflowDefinitionService {

    private static final int DRAFT = 0;
    private static final int PUBLISHED = 1;
    private static final int EXPIRED = 9;
    private static final String FLOW_CODE_PATTERN = "^[A-Za-z][A-Za-z0-9_-]{1,39}$";

    private final WorkflowCategoryMapper categoryMapper;
    private final WorkflowDefinitionUsageChecker usageChecker;
    private final WorkflowModelValidator modelValidator;

    public WorkflowDefinitionServiceImpl(WorkflowCategoryMapper categoryMapper,
                                         WorkflowDefinitionUsageChecker usageChecker,
                                         WorkflowModelValidator modelValidator) {
        this.categoryMapper = categoryMapper;
        this.usageChecker = usageChecker;
        this.modelValidator = modelValidator;
    }

    @Override
    public PageResult<WorkflowDefinitionVO> page(Integer pageNum, Integer pageSize, String flowCode, String flowName,
                                                 Long categoryId, Integer publishStatus) {
        Collection<Long> categoryIds = resolveCategoryIds(categoryId);
        if (categoryId != null && categoryIds.isEmpty()) {
            return PageResult.of(new Page<>(pageNum, pageSize, 0), Collections.emptyList());
        }
        LambdaQueryWrapper<WorkflowDefinitionDO> wrapper = new LambdaQueryWrapper<WorkflowDefinitionDO>()
                .like(StrUtil.isNotBlank(flowCode), WorkflowDefinitionDO::getFlowCode, StrUtil.trim(flowCode))
                .like(StrUtil.isNotBlank(flowName), WorkflowDefinitionDO::getFlowName, StrUtil.trim(flowName))
                .in(categoryId != null, WorkflowDefinitionDO::getCategoryId, categoryIds)
                .eq(publishStatus != null, WorkflowDefinitionDO::getPublishStatus, publishStatus)
                .orderByDesc(WorkflowDefinitionDO::getCreateTime)
                .orderByDesc(WorkflowDefinitionDO::getDefinitionId);
        Page<WorkflowDefinitionDO> page = baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, String> categoryNames = categoryNames(page.getRecords());
        return PageResult.of(page, page.getRecords().stream()
                .map(definition -> toVO(definition, categoryNames.get(definition.getCategoryId())))
                .toList());
    }

    @Override
    public WorkflowDefinitionVO getRequired(Long definitionId) {
        WorkflowDefinitionDO definition = getRequiredDO(definitionId);
        WorkflowCategoryDO category = categoryMapper.selectById(definition.getCategoryId());
        return toVO(definition, category == null ? null : category.getCategoryName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WorkflowDefinitionCreateRequest request) {
        String flowCode = StrUtil.trim(request.getFlowCode());
        String flowName = StrUtil.trim(request.getFlowName());
        validateCreateFields(flowCode, flowName, request.getCategoryId());
        String modelJson = modelValidator.normalizeAndValidate(request.getModelJson());
        List<WorkflowDefinitionDO> versions = listVersions(flowCode);
        if (versions.stream().anyMatch(item -> Objects.equals(item.getPublishStatus(), DRAFT))) {
            throw new BusinessException("该流程编码已有草稿版本");
        }
        int nextVersion = versions.stream()
                .map(WorkflowDefinitionDO::getVersionNo)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        WorkflowDefinitionDO definition = new WorkflowDefinitionDO()
                .setFlowCode(flowCode)
                .setFlowName(flowName)
                .setCategoryId(request.getCategoryId())
                .setVersionNo(nextVersion)
                .setPublishStatus(DRAFT)
                .setActivityStatus(1)
                .setFormPath(StrUtil.trim(request.getFormPath()))
                .setModelJson(modelJson)
                .setRemark(StrUtil.trim(request.getRemark()));
        baseMapper.insert(definition);
        return definition.getDefinitionId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(WorkflowDefinitionUpdateRequest request) {
        WorkflowDefinitionDO definition = getRequiredDO(request.getDefinitionId());
        requireDraft(definition);
        getRequiredCategory(request.getCategoryId());
        definition.setFlowName(StrUtil.trim(request.getFlowName()));
        definition.setCategoryId(request.getCategoryId());
        definition.setFormPath(StrUtil.trim(request.getFormPath()));
        definition.setModelJson(modelValidator.normalizeAndValidate(request.getModelJson()));
        definition.setRemark(StrUtil.trim(request.getRemark()));
        baseMapper.updateById(definition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long definitionId) {
        WorkflowDefinitionDO definition = getRequiredDO(definitionId);
        requireDraft(definition);
        modelValidator.normalizeAndValidate(definition.getModelJson());
        for (WorkflowDefinitionDO published : baseMapper.selectList(new LambdaQueryWrapper<WorkflowDefinitionDO>()
                .eq(WorkflowDefinitionDO::getFlowCode, definition.getFlowCode())
                .eq(WorkflowDefinitionDO::getPublishStatus, PUBLISHED))) {
            if (!Objects.equals(published.getDefinitionId(), definitionId)) {
                published.setPublishStatus(EXPIRED);
                published.setActivityStatus(0);
                baseMapper.updateById(published);
            }
        }
        definition.setPublishStatus(PUBLISHED);
        definition.setActivityStatus(1);
        baseMapper.updateById(definition);
    }

    @Override
    public void unpublish(Long definitionId) {
        WorkflowDefinitionDO definition = getRequiredDO(definitionId);
        if (!Objects.equals(definition.getPublishStatus(), PUBLISHED)) {
            throw new BusinessException("只有已发布版本可以取消发布");
        }
        Long draftCount = baseMapper.selectCount(new LambdaQueryWrapper<WorkflowDefinitionDO>()
                .eq(WorkflowDefinitionDO::getFlowCode, definition.getFlowCode())
                .eq(WorkflowDefinitionDO::getPublishStatus, DRAFT)
                .ne(WorkflowDefinitionDO::getDefinitionId, definitionId));
        if (draftCount > 0) {
            throw new BusinessException("该流程编码已有草稿版本，不能取消发布");
        }
        if (usageChecker.isUsed(definitionId)) {
            throw new BusinessException("流程定义已有运行实例，不能取消发布");
        }
        definition.setPublishStatus(DRAFT);
        definition.setActivityStatus(1);
        baseMapper.updateById(definition);
    }

    @Override
    public void setActive(Long definitionId, boolean active) {
        WorkflowDefinitionDO definition = getRequiredDO(definitionId);
        if (!Objects.equals(definition.getPublishStatus(), PUBLISHED)) {
            throw new BusinessException("只有已发布版本可以激活或挂起");
        }
        definition.setActivityStatus(active ? 1 : 0);
        baseMapper.updateById(definition);
    }

    @Override
    public Long copy(WorkflowDefinitionCopyRequest request) {
        WorkflowDefinitionDO source = getRequiredDO(request.getSourceDefinitionId());
        WorkflowDefinitionCreateRequest createRequest = new WorkflowDefinitionCreateRequest();
        createRequest.setFlowCode(request.getFlowCode());
        createRequest.setFlowName(request.getFlowName());
        createRequest.setCategoryId(source.getCategoryId());
        createRequest.setFormPath(source.getFormPath());
        createRequest.setModelJson(source.getModelJson());
        createRequest.setRemark(source.getRemark());
        return create(createRequest);
    }

    @Override
    public Long importJson(String json, Long categoryId) {
        try {
            JSONObject payload = JSONUtil.parseObj(json);
            WorkflowDefinitionCreateRequest request = new WorkflowDefinitionCreateRequest();
            request.setFlowCode(payload.getStr("flowCode"));
            request.setFlowName(payload.getStr("flowName"));
            request.setCategoryId(categoryId == null ? payload.getLong("categoryId") : categoryId);
            request.setFormPath(payload.getStr("formPath"));
            Object model = payload.get("model");
            request.setModelJson(model instanceof String text ? text : JSONUtil.toJsonStr(model));
            request.setRemark(payload.getStr("remark"));
            return create(request);
        } catch (JSONException exception) {
            throw new BusinessException("流程定义导入文件不是有效的 JSON");
        }
    }

    @Override
    public String exportJson(Long definitionId) {
        WorkflowDefinitionDO definition = getRequiredDO(definitionId);
        JSONObject payload = new JSONObject();
        payload.set("flowCode", definition.getFlowCode());
        payload.set("flowName", definition.getFlowName());
        payload.set("categoryId", definition.getCategoryId());
        payload.set("formPath", definition.getFormPath());
        payload.set("remark", definition.getRemark());
        payload.set("model", JSONUtil.parse(definition.getModelJson()));
        return JSONUtil.toJsonPrettyStr(payload);
    }

    @Override
    public void delete(Long definitionId) {
        WorkflowDefinitionDO definition = getRequiredDO(definitionId);
        if (Objects.equals(definition.getPublishStatus(), PUBLISHED)) {
            throw new BusinessException("已发布流程定义不能删除");
        }
        if (usageChecker.isUsed(definitionId)) {
            throw new BusinessException("流程定义已有运行实例，不能删除");
        }
        baseMapper.deleteById(definitionId);
    }

    private WorkflowDefinitionDO getRequiredDO(Long definitionId) {
        WorkflowDefinitionDO definition = baseMapper.selectById(definitionId);
        if (definition == null) {
            throw new BusinessException("流程定义不存在");
        }
        return definition;
    }

    private void requireDraft(WorkflowDefinitionDO definition) {
        if (!Objects.equals(definition.getPublishStatus(), DRAFT)) {
            throw new BusinessException("只有草稿版本可以修改或发布");
        }
    }

    private void validateCreateFields(String flowCode, String flowName, Long categoryId) {
        if (StrUtil.isBlank(flowCode) || !ReUtil.isMatch(FLOW_CODE_PATTERN, flowCode)) {
            throw new BusinessException("流程编码格式不正确");
        }
        if (StrUtil.isBlank(flowName)) {
            throw new BusinessException("流程名称不能为空");
        }
        getRequiredCategory(categoryId);
    }

    private WorkflowCategoryDO getRequiredCategory(Long categoryId) {
        WorkflowCategoryDO category = categoryId == null ? null : categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("流程分类不存在");
        }
        return category;
    }

    private List<WorkflowDefinitionDO> listVersions(String flowCode) {
        return baseMapper.selectList(new LambdaQueryWrapper<WorkflowDefinitionDO>()
                .eq(WorkflowDefinitionDO::getFlowCode, flowCode)
                .orderByDesc(WorkflowDefinitionDO::getVersionNo));
    }

    private Collection<Long> resolveCategoryIds(Long categoryId) {
        if (categoryId == null) {
            return Collections.emptyList();
        }
        String categoryText = String.valueOf(categoryId);
        return categoryMapper.selectList(null).stream()
                .filter(category -> Objects.equals(category.getCategoryId(), categoryId)
                        || Arrays.stream(StrUtil.nullToEmpty(category.getAncestors()).split(","))
                        .map(String::trim)
                        .anyMatch(categoryText::equals))
                .map(WorkflowCategoryDO::getCategoryId)
                .toList();
    }

    private Map<Long, String> categoryNames(List<WorkflowDefinitionDO> definitions) {
        Set<Long> categoryIds = definitions.stream()
                .map(WorkflowDefinitionDO::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(WorkflowCategoryDO::getCategoryId,
                        WorkflowCategoryDO::getCategoryName, (left, right) -> left));
    }

    private WorkflowDefinitionVO toVO(WorkflowDefinitionDO definition, String categoryName) {
        return new WorkflowDefinitionVO()
                .setDefinitionId(definition.getDefinitionId())
                .setFlowCode(definition.getFlowCode())
                .setFlowName(definition.getFlowName())
                .setCategoryId(definition.getCategoryId())
                .setCategoryName(categoryName)
                .setVersionNo(definition.getVersionNo())
                .setPublishStatus(definition.getPublishStatus())
                .setActivityStatus(definition.getActivityStatus())
                .setFormPath(definition.getFormPath())
                .setModelJson(definition.getModelJson())
                .setRemark(definition.getRemark())
                .setCreateTime(definition.getCreateTime())
                .setUpdateTime(definition.getUpdateTime());
    }
}
