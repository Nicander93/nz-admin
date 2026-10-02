package com.nz.admin.framework.datascope.core;

import java.util.Set;

/** 已解析的数据范围；全部数据仍受独立的租户隔离约束。 */
public record DataScopeResult(boolean all, Set<Long> deptIds, boolean self, Long userId) {
    public DataScopeResult {
        deptIds = Set.copyOf(deptIds);
        if (self && userId == null) {
            throw new IllegalArgumentException("本人范围缺少用户 ID");
        }
    }
}
