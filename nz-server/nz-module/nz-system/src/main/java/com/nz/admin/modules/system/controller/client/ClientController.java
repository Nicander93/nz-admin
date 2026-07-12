package com.nz.admin.modules.system.controller.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.convert.client.ClientConvert;
import com.nz.admin.modules.system.entity.dataobject.client.ClientDO;
import com.nz.admin.modules.system.entity.dto.client.ClientCreateRequest;
import com.nz.admin.modules.system.entity.dto.client.ClientUpdateRequest;
import com.nz.admin.modules.system.entity.vo.client.ClientVO;
import com.nz.admin.modules.system.service.client.ClientService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 客户端管理接口。
 */
@RestController
@RequestMapping("/api/system/client")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @SaCheckPermission("system:client:list")
    @GetMapping("/page")
    public R<PageResult<ClientVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String clientId,
                                        @RequestParam(required = false) String clientName,
                                        @RequestParam(required = false) Integer status) {
        Page<ClientDO> page = clientService.page(pageNum, pageSize, clientId, clientName, status);
        return R.ok(PageResult.of(page, ClientConvert.toVOList(page.getRecords())));
    }

    @SaCheckPermission("system:client:query")
    @GetMapping("/{id}")
    public R<ClientVO> get(@PathVariable Long id) {
        return R.ok(ClientConvert.toVO(clientService.getRequired(id)));
    }

    @Log(title = "客户端管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:client:add")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ClientCreateRequest request) {
        return R.ok(clientService.create(request));
    }

    @Log(title = "客户端管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:client:edit")
    @PutMapping
    public R<Void> update(@Valid @RequestBody ClientUpdateRequest request) {
        clientService.update(request);
        return R.ok();
    }

    @Log(title = "客户端管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:client:remove")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return R.ok();
    }
}
