package com.nz.admin.framework.mybatis.core.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.Collection;

/**
 * 常用条件拼装扩展。
 */
public class LambdaQueryWrapperX<T> extends LambdaQueryWrapper<T> {

    public LambdaQueryWrapperX<T> eqIfPresent(SFunction<T, ?> column, Object value) {
        return (LambdaQueryWrapperX<T>) super.eq(value != null, column, value);
    }

    public LambdaQueryWrapperX<T> likeIfPresent(SFunction<T, ?> column, String value) {
        return (LambdaQueryWrapperX<T>) super.like(value != null && !value.isBlank(), column, value);
    }

    public LambdaQueryWrapperX<T> betweenIfPresent(SFunction<T, ?> column, Object begin, Object end) {
        if (begin != null && end != null) {
            super.between(column, begin, end);
        } else if (begin != null) {
            super.ge(column, begin);
        } else if (end != null) {
            super.le(column, end);
        }
        return this;
    }

    public LambdaQueryWrapperX<T> inIfPresent(SFunction<T, ?> column, Collection<?> values) {
        return (LambdaQueryWrapperX<T>) super.in(values != null && !values.isEmpty(), column, values);
    }

    @SafeVarargs
    public final LambdaQueryWrapperX<T> orderByAscIfPresent(SFunction<T, ?>... columns) {
        if (columns != null && columns.length > 0) {
            SFunction<T, ?> first = columns[0];
            SFunction<T, ?>[] remaining = new SFunction[Math.max(columns.length - 1, 0)];
            if (columns.length > 1) {
                System.arraycopy(columns, 1, remaining, 0, columns.length - 1);
            }
            super.orderByAsc(first, remaining);
        }
        return this;
    }

    @SafeVarargs
    public final LambdaQueryWrapperX<T> orderByDescIfPresent(SFunction<T, ?>... columns) {
        if (columns != null && columns.length > 0) {
            SFunction<T, ?> first = columns[0];
            SFunction<T, ?>[] remaining = new SFunction[Math.max(columns.length - 1, 0)];
            if (columns.length > 1) {
                System.arraycopy(columns, 1, remaining, 0, columns.length - 1);
            }
            super.orderByDesc(first, remaining);
        }
        return this;
    }
}
