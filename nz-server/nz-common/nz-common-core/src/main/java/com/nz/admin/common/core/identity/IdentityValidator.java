package com.nz.admin.common.core.identity;

/** 由身份所属业务核对当前状态；存储不可用应传播错误，不能默认放行。 */
@FunctionalInterface
public interface IdentityValidator {
    boolean isValid(IdentitySnapshot identity);
}
