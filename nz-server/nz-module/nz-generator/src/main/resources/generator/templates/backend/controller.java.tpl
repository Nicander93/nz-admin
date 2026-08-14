package @@PACKAGE@@.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import @@PACKAGE@@.convert.@@CLASS@@Convert;
import @@PACKAGE@@.entity.dataobject.@@CLASS@@DO;
import @@PACKAGE@@.entity.dto.@@CLASS@@CreateRequest;
import @@PACKAGE@@.entity.dto.@@CLASS@@UpdateRequest;
import @@PACKAGE@@.entity.query.@@CLASS@@Query;
import @@PACKAGE@@.entity.vo.@@CLASS@@VO;
import @@PACKAGE@@.service.@@CLASS@@Service;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @@FEATURE_DOC@@接口。
 *
 * @author @@AUTHOR@@
 */
@RestController
@RequestMapping("/api/@@MODULE@@/@@BUSINESS@@")
public class @@CLASS@@Controller {

    private final @@CLASS@@Service @@CLASS_CAMEL@@Service;

    public @@CLASS@@Controller(@@CLASS@@Service @@CLASS_CAMEL@@Service) {
        this.@@CLASS_CAMEL@@Service = @@CLASS_CAMEL@@Service;
    }

    @SaCheckPermission("@@PERMISSION_PREFIX@@:list")
    @GetMapping("/page")
    public R<PageResult<@@CLASS@@VO>> page(@Valid @@CLASS@@Query query) {
        Page<@@CLASS@@DO> page = @@CLASS_CAMEL@@Service.page(query);
        return R.ok(PageResult.of(page, @@CLASS@@Convert.toVOList(page.getRecords())));
    }

    @SaCheckPermission("@@PERMISSION_PREFIX@@:query")
    @GetMapping("/{@@PK_FIELD@@}")
    public R<@@CLASS@@VO> get(@PathVariable @@PK_TYPE@@ @@PK_FIELD@@) {
        return R.ok(@@CLASS@@Convert.toVO(@@CLASS_CAMEL@@Service.getRequired(@@PK_FIELD@@)));
    }

    @Log(title = "@@FEATURE_JAVA@@", businessType = BusinessType.INSERT)
    @SaCheckPermission("@@PERMISSION_PREFIX@@:add")
    @PostMapping
    public R<@@PK_TYPE@@> create(@Valid @RequestBody @@CLASS@@CreateRequest request) {
        return R.ok(@@CLASS_CAMEL@@Service.create(request));
    }

    @Log(title = "@@FEATURE_JAVA@@", businessType = BusinessType.UPDATE)
    @SaCheckPermission("@@PERMISSION_PREFIX@@:edit")
    @PutMapping
    public R<Void> update(@Valid @RequestBody @@CLASS@@UpdateRequest request) {
        @@CLASS_CAMEL@@Service.update(request);
        return R.ok();
    }

    @Log(title = "@@FEATURE_JAVA@@", businessType = BusinessType.DELETE)
    @SaCheckPermission("@@PERMISSION_PREFIX@@:remove")
    @DeleteMapping("/{@@PK_FIELD@@}")
    public R<Void> delete(@PathVariable @@PK_TYPE@@ @@PK_FIELD@@) {
        @@CLASS_CAMEL@@Service.delete(@@PK_FIELD@@);
        return R.ok();
    }
}
