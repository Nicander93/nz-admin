package com.nz.admin.modules.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.demo.entity.dataobject.DemoItemDO;
import com.nz.admin.modules.demo.entity.dto.DemoItemCreateRequest;
import com.nz.admin.modules.demo.entity.dto.DemoItemUpdateRequest;

/**
 * 示例条目服务。
 */
public interface DemoItemService {

    Page<DemoItemDO> page(Integer pageNum, Integer pageSize, String name, String category, Integer status);

    DemoItemDO getRequired(Long id);

    Long create(DemoItemCreateRequest request);

    void update(DemoItemUpdateRequest request);

    void delete(Long id);
}
