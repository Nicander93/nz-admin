package com.nz.admin.modules.system.service.log;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.log.OperLogDO;
import com.nz.admin.modules.system.entity.query.log.OperLogQuery;

import java.time.LocalDateTime;
import java.util.List;

public interface OperLogService {

    Page<OperLogDO> listPage(OperLogQuery query);

    OperLogDO getById(Long id);

    void removeByIds(List<Long> ids);

    int removeBefore(LocalDateTime deadline);

    void saveAsync(OperLogDO operLog);
}
