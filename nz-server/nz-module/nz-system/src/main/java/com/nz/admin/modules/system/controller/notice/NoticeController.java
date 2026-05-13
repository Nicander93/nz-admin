package com.nz.admin.modules.system.controller.notice;

import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.dataobject.notice.NoticeDO;
import com.nz.admin.modules.system.entity.query.notice.NoticeQuery;
import com.nz.admin.modules.system.service.notice.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 通知公告管理接口。
 */
@RestController
@RequestMapping("/api/system/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /**
     * 分页查询通知公告。
     */
    @SaCheckPermission("system:notice:list")
    @Log(title = "通知公告", businessType = BusinessType.QUERY)
    @GetMapping("/page")
    public R<PageResult<NoticeDO>> page(NoticeQuery query) {
        return R.ok(PageResult.of(noticeService.listPage(query)));
    }

    /**
     * 按 id 查询通知公告详情。
     */
    @SaCheckPermission("system:notice:query")
    @Log(title = "通知公告", businessType = BusinessType.QUERY)
    @GetMapping("/{id}")
    public R<NoticeDO> getById(@PathVariable Long id) {
        return R.ok(noticeService.getById(id));
    }

    /**
     * 新增通知公告。
     */
    @SaCheckPermission("system:notice:add")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody NoticeDO notice) {
        noticeService.save(notice);
        return R.ok();
    }

    /**
     * 更新通知公告。
     */
    @SaCheckPermission("system:notice:edit")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> update(@RequestBody NoticeDO notice) {
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
