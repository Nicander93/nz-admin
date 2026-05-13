package com.nz.admin.common.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RTest {

    @Test
    void okWithoutData() {
        R<Void> r = R.ok();
        assertEquals(200, r.getCode());
        assertEquals("success", r.getMsg());
        assertNull(r.getData());
    }

    @Test
    void okWithData() {
        R<String> r = R.ok("hello");
        assertEquals(200, r.getCode());
        assertEquals("hello", r.getData());
    }

    @Test
    void failWithMessage() {
        R<Object> r = R.fail("bad");
        assertEquals(500, r.getCode());
        assertEquals("bad", r.getMsg());
    }

    @Test
    void failWithCode() {
        R<Object> r = R.fail(403, "denied");
        assertEquals(403, r.getCode());
        assertEquals("denied", r.getMsg());
    }

    @Test
    void failWithErrorCode() {
        R<Object> r = R.fail(CommonErrorCode.FORBIDDEN);
        assertEquals(403, r.getCode());
        assertEquals("没有访问权限", r.getMsg());
    }

    @Test
    void failWithBusinessException() {
        R<Object> r = R.fail(new BusinessException(409, "状态冲突"));
        assertEquals(409, r.getCode());
        assertEquals("状态冲突", r.getMsg());
    }
}
