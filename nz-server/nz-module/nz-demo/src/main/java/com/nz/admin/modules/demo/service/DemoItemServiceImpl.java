package com.nz.admin.modules.demo.service;

import cn.hutool.core.util.StrUtil;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.excel.core.ExcelImportError;
import com.nz.admin.modules.demo.convert.DemoItemConvert;
import com.nz.admin.modules.demo.entity.dataobject.DemoItemDO;
import com.nz.admin.modules.demo.entity.dto.DemoItemCreateRequest;
import com.nz.admin.modules.demo.entity.dto.DemoItemUpdateRequest;
import com.nz.admin.modules.demo.entity.excel.DemoItemExcelRow;
import com.nz.admin.modules.demo.entity.vo.DemoItemExcelExportRow;
import com.nz.admin.modules.demo.entity.vo.DemoItemImportResult;
import com.nz.admin.modules.demo.mapper.DemoItemMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 示例条目服务实现。
 */
@Service
public class DemoItemServiceImpl extends ServiceImpl<DemoItemMapper, DemoItemDO> implements DemoItemService {

    private static final int MAX_IMPORT_ROWS = 1000;
    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_EXPORT_ROWS = 5000;

    private final com.nz.admin.framework.auth.core.LoginUserContext loginUserContext;
    private final Validator validator;

    public DemoItemServiceImpl(com.nz.admin.framework.auth.core.LoginUserContext loginUserContext, Validator validator) {
        this.loginUserContext = loginUserContext;
        this.validator = validator;
    }

    @Override
    public Page<DemoItemDO> page(Integer pageNum, Integer pageSize, String name, String category, Integer status) {
        return baseMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper(name, category, status));
    }

    @Override
    public DemoItemDO getRequired(Long id) {
        DemoItemDO item = baseMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("示例条目不存在");
        }
        return item;
    }

    @Override
    public Long create(DemoItemCreateRequest request) {
        DemoItemDO item = DemoItemConvert.toDO(request);
        Long userId = loginUserContext.getLoginUserIdOrNull();
        if (userId == null) {
            throw new BusinessException("创建示例条目需要登录身份");
        }
        item.setOwnerId(userId);
        baseMapper.insert(item);
        return item.getId();
    }

    @Override
    public void update(DemoItemUpdateRequest request) {
        getRequired(request.getId());
        if (baseMapper.updateById(DemoItemConvert.toDO(request)) != 1) {
            throw new BusinessException("示例条目不存在或无权修改");
        }
    }

    @Override
    public void delete(Long id) {
        getRequired(id);
        if (baseMapper.deleteById(id) != 1) {
            throw new BusinessException("示例条目不存在或无权删除");
        }
    }
    private LambdaQueryWrapper<DemoItemDO> queryWrapper(String name, String category, Integer status) {
        return new LambdaQueryWrapper<DemoItemDO>()
                .like(StrUtil.isNotBlank(name), DemoItemDO::getName, name)
                .eq(StrUtil.isNotBlank(category), DemoItemDO::getCategory, category)
                .eq(status != null, DemoItemDO::getStatus, status)
                .orderByAsc(DemoItemDO::getSort)
                .orderByDesc(DemoItemDO::getId);
    }

    @Override
    public DemoItemImportResult previewImport(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new BusinessException("请选择非空 Excel 文件");
        if (file.getSize() > MAX_FILE_BYTES) throw new BusinessException("Excel 文件不能超过 10MB");
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessException("仅支持 .xlsx 文件");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] signature = input.readNBytes(4);
            if (signature.length < 4 || signature[0] != 'P' || signature[1] != 'K') {
                throw new BusinessException("文件不是有效的 xlsx 工作簿");
            }
        }
        ImportListener listener = new ImportListener();
        try {
            EasyExcel.read(file.getInputStream(), DemoItemExcelRow.class, listener)
                    .headRowNumber(1).sheet(0).doRead();
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException("Excel 文件无法解析，请检查文件格式和表头");
        }
        return listener.result();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importItems(List<DemoItemCreateRequest> rows) {
        if (rows == null || rows.isEmpty()) throw new BusinessException("没有可导入的数据");
        if (rows.size() > MAX_IMPORT_ROWS) throw new BusinessException("一次最多导入 1000 行");
        List<ExcelImportError> errors = validateRows(rows);
        if (!errors.isEmpty()) {
            throw new BusinessException(errors.stream().map(error -> "第 " + error.getRowNum() + " 行：" + error.getMessage())
                    .collect(java.util.stream.Collectors.joining("；")));
        }
        Long userId = loginUserContext.getLoginUserIdOrNull();
        if (userId == null) throw new BusinessException("导入示例条目需要登录身份");
        for (DemoItemCreateRequest request : rows) {
            DemoItemDO item = DemoItemConvert.toDO(request);
            item.setOwnerId(userId);
            if (baseMapper.insert(item) != 1) throw new BusinessException("导入失败，已回滚全部行");
        }
        return rows.size();
    }

    @Override
    public List<DemoItemExcelExportRow> exportItems(String name, String category, Integer status) {
        if (status != null && status != 0 && status != 1) throw new BusinessException("状态值不正确");
        Page<DemoItemDO> page = baseMapper.selectPage(new Page<>(1, MAX_EXPORT_ROWS + 1), queryWrapper(name, category, status));
        if (page.getTotal() > MAX_EXPORT_ROWS) throw new BusinessException("筛选结果超过 5000 行，请增加筛选条件后导出");
        return page.getRecords().stream().map(item -> {
            DemoItemExcelExportRow row = new DemoItemExcelExportRow();
            row.setName(excelText(item.getName()));
            row.setCategory(excelText(item.getCategory()));
            row.setStatus(item.getStatus());
            row.setSort(item.getSort());
            row.setRemark(excelText(item.getRemark()));
            return row;
        }).toList();
    }

    String excelText(String value) {
        if (value != null && !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) return "'" + value;
        return value;
    }

    private List<ExcelImportError> validateRows(List<DemoItemCreateRequest> rows) {
        List<ExcelImportError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            DemoItemCreateRequest row = rows.get(i);
            if (row == null) {
                errors.add(new ExcelImportError(i + 2, "行数据不能为空"));
                continue;
            }
            for (ConstraintViolation<DemoItemCreateRequest> violation : validator.validate(row)) {
                errors.add(new ExcelImportError(i + 2, violation.getMessage()));
            }
        }
        return errors;
    }

    private final class ImportListener extends AnalysisEventListener<DemoItemExcelRow> {
        private final List<DemoItemCreateRequest> rows = new ArrayList<>();
        private final List<ExcelImportError> errors = new ArrayList<>();
        private boolean validHeader;
        private int rowCount;

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            List<String> required = List.of("条目名称", "分类", "状态（0启用，1停用）", "排序（0-999）", "备注");
            validHeader = required.stream().allMatch(requiredName -> headMap.containsValue(requiredName))
                    && required.size() == headMap.size()
                    && java.util.stream.IntStream.range(0, required.size())
                    .allMatch(index -> required.get(index).equals(headMap.get(index)));
            if (!validHeader) throw new BusinessException("表头不完整，请使用系统提供的导入模板");
        }

        @Override
        public void invoke(DemoItemExcelRow excelRow, AnalysisContext context) {
            countRow();
            int sheetRowNumber = context.readRowHolder().getRowIndex() + 1;
            DemoItemCreateRequest row = new DemoItemCreateRequest();
            row.setName(excelRow.getName() == null ? null : excelRow.getName().trim());
            row.setCategory(excelRow.getCategory() == null ? null : excelRow.getCategory().trim());
            row.setStatus(excelRow.getStatus());
            row.setSort(excelRow.getSort());
            row.setRemark(excelRow.getRemark());
            List<ExcelImportError> rowErrors = validateRows(List.of(row));
            if (rowErrors.isEmpty()) rows.add(row);
            else rowErrors.forEach(error -> errors.add(new ExcelImportError(sheetRowNumber, error.getMessage())));
        }

        @Override
        public void onException(Exception exception, AnalysisContext context) throws Exception {
            if (exception instanceof ExcelDataConvertException conversion) {
                countRow();
                errors.add(new ExcelImportError(conversion.getRowIndex() + 1,
                        "第 " + (conversion.getColumnIndex() + 1) + " 列数据格式不正确，请检查状态和排序是否为整数"));
                return;
            }
            throw exception;
        }

        private void countRow() {
            if (++rowCount > MAX_IMPORT_ROWS) throw new BusinessException("一次最多导入 1000 行");
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            if (!validHeader) throw new BusinessException("Excel 表头缺失");
            if (rowCount == 0) throw new BusinessException("Excel 文件没有数据行");
        }

        DemoItemImportResult result() {
            return new DemoItemImportResult(rowCount, rows.size(), rows, errors);
        }
    }

}
