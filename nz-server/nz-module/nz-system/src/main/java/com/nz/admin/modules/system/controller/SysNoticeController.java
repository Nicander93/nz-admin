package com.nz.admin.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.po.SysNoticeDO;
import com.nz.admin.modules.system.entity.query.SysNoticeQuery;
import com.nz.admin.modules.system.service.SysNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 通知公告管理接口。
 */
@RestController
@RequestMapping("/api/system/notice")
public class SysNoticeController {

    @Autowired
    private SysNoticeService noticeService;

    /**
     * 分页查询通知公告。
     */
    @SaCheckPermission("system:notice:list")
    @Log(title = "通知公告", businessType = BusinessType.QUERY)
    @GetMapping("/page")
    public R<Page<SysNoticeDO>> page(SysNoticeQuery query) {
        return R.ok(noticeService.listPage(query));
    }

    /**
     * 按 id 查询通知公告详情。
     */
    @SaCheckPermission("system:notice:query")
    @Log(title = "通知公告", businessType = BusinessType.QUERY)
    @GetMapping("/{id}")
    public R<SysNoticeDO> getById(@PathVariable Long id) {
        return R.ok(noticeService.getById(id));
    }

    /**
     * 新增通知公告。
     */
    @SaCheckPermission("system:notice:add")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody SysNoticeDO notice) {
        noticeService.save(notice);
        return R.ok();
    }

    /**
     * 更新通知公告。
     */
    @SaCheckPermission("system:notice:edit")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> update(@RequestBody SysNoticeDO notice) {
        noticeService.updateById(notice);
        return R.ok();
    }

    /**
     * 删除通知公告。
     */
    @SaCheckPermission("system:notice:remove")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        return R.ok();
    }
}
