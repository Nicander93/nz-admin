package com.nz.admin.framework.datascope;

/**
 * 数据权限用户信息解析器。
 */
public interface DataScopeUserResolver {

    /**
     * 判断当前用户是否管理员。
     *
     * @param userId 用户 ID
     * @return 是否管理员
     */
    boolean isAdmin(Long userId);

    /**
     * 查询当前用户所属部门 ID。
     *
     * @param userId 用户 ID
     * @return 部门 ID
     */
    Long getDeptId(Long userId);
}
