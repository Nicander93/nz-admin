package com.nz.admin.modules.system.service.log;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.log.LoginLogDO;
import com.nz.admin.modules.system.entity.query.log.LoginLogQuery;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginLogService {

    Page<LoginLogDO> listPage(LoginLogQuery query);

    LoginLogDO getById(Long id);

    void saveAsync(LoginLogDO loginLog);

    void removeByIds(List<Long> ids);

    int removeBefore(LocalDateTime deadline);
}
