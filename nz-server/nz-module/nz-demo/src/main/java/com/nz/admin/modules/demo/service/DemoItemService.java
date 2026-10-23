package com.nz.admin.modules.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.demo.entity.dataobject.DemoItemDO;
import com.nz.admin.modules.demo.entity.dto.DemoItemCreateRequest;
import com.nz.admin.modules.demo.entity.dto.DemoItemUpdateRequest;
import com.nz.admin.modules.demo.entity.vo.DemoItemExcelExportRow;
import com.nz.admin.modules.demo.entity.vo.DemoItemImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 示例条目服务。
 */
public interface DemoItemService {

    Page<DemoItemDO> page(Integer pageNum, Integer pageSize, String name, String category, Integer status);

    DemoItemDO getRequired(Long id);

    Long create(DemoItemCreateRequest request);

    void update(DemoItemUpdateRequest request);

    void delete(Long id);

    DemoItemImportResult previewImport(MultipartFile file) throws IOException;

    int importItems(List<DemoItemCreateRequest> rows);

    List<DemoItemExcelExportRow> exportItems(String name, String category, Integer status);
}
