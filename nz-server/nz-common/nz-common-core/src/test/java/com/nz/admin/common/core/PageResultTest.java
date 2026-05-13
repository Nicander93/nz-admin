package com.nz.admin.common.core;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageResultTest {

    @Test
    void shouldConvertMybatisPageToPageResult() {
        Page<String> page = new Page<>(2, 5, 11);
        page.setRecords(List.of("a", "b"));

        PageResult<String> result = PageResult.of(page);

        assertEquals(List.of("a", "b"), result.getRecords());
        assertEquals(11, result.getTotal());
        assertEquals(5, result.getSize());
        assertEquals(2, result.getCurrent());
        assertEquals(3, result.getPages());
    }
}
