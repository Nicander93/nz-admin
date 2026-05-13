package com.nz.admin.common.core;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果。
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long size;
    private long current;
    private long pages;

    public static <T> PageResult<T> empty() {
        PageResult<T> result = new PageResult<>();
        result.setRecords(Collections.emptyList());
        return result;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        if (page == null) {
            return empty();
        }

        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setSize(page.getSize());
        result.setCurrent(page.getCurrent());
        result.setPages(page.getPages());
        return result;
    }

    public static <S, T> PageResult<T> of(IPage<S> page, List<T> records) {
        if (page == null) {
            return empty();
        }

        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(page.getTotal());
        result.setSize(page.getSize());
        result.setCurrent(page.getCurrent());
        result.setPages(page.getPages());
        return result;
    }
}
