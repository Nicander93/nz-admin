package com.nz.admin.framework.mybatis.dataobject;

import com.nz.admin.common.core.BaseEntity;

import java.io.Serializable;

/**
 * 持久层 DO 基类，继承 {@link BaseEntity}，与业务里「DO」命名习惯对齐，后续可在本类集中扩展公共表字段。
 */
public abstract class BaseDO extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;
}
