package com.nz.admin.modules.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.demo.convert.DemoItemConvert;
import com.nz.admin.modules.demo.entity.dataobject.DemoItemDO;
import com.nz.admin.modules.demo.entity.dto.DemoItemCreateRequest;
import com.nz.admin.modules.demo.entity.dto.DemoItemUpdateRequest;
import com.nz.admin.modules.demo.entity.vo.DemoItemVO;
import com.nz.admin.modules.demo.service.DemoItemService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例条目接口。
 */
@RestController
@RequestMapping("/api/demo/item")
public class DemoItemController {

    private final DemoItemService demoItemService;

    public DemoItemController(DemoItemService demoItemService) {
        this.demoItemService = demoItemService;
    }

    @SaCheckPermission("demo:item:list")
    @GetMapping("/page")
    public R<PageResult<DemoItemVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(required = false) Integer status) {
        Page<DemoItemDO> page = demoItemService.page(pageNum, pageSize, name, category, status);
        return R.ok(PageResult.of(page, DemoItemConvert.toVOList(page.getRecords())));
    }

    @SaCheckPermission("demo:item:query")
    @GetMapping("/{id}")
    public R<DemoItemVO> get(@PathVariable Long id) {
        return R.ok(DemoItemConvert.toVO(demoItemService.getRequired(id)));
    }

    @Log(title = "示例条目", businessType = BusinessType.INSERT)
    @SaCheckPermission("demo:item:add")
    @PostMapping
    public R<Long> create(@Valid @RequestBody DemoItemCreateRequest request) {
        return R.ok(demoItemService.create(request));
    }

    @Log(title = "示例条目", businessType = BusinessType.UPDATE)
    @SaCheckPermission("demo:item:edit")
    @PutMapping
    public R<Void> update(@Valid @RequestBody DemoItemUpdateRequest request) {
        demoItemService.update(request);
        return R.ok();
    }

    @Log(title = "示例条目", businessType = BusinessType.DELETE)
    @SaCheckPermission("demo:item:remove")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        demoItemService.delete(id);
        return R.ok();
    }
}
