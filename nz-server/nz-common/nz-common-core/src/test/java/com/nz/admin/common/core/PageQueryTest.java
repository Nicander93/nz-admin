package com.nz.admin.common.core;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageQueryTest {

    @Test
    void defaultPaging() {
        PageQuery q = new PageQuery();
        assertEquals(1, q.getPageNum());
        assertEquals(10, q.getPageSize());
    }

    @Test
    void toPageUsesCurrentValues() {
        PageQuery q = new PageQuery();
        q.setPageNum(2);
        q.setPageSize(20);
        Page<String> page = q.toPage();
        assertEquals(2L, page.getCurrent());
        assertEquals(20L, page.getSize());
    }
}
