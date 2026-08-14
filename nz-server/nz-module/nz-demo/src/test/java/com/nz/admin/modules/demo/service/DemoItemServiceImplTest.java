package com.nz.admin.modules.demo.service;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.demo.entity.dataobject.DemoItemDO;
import com.nz.admin.modules.demo.entity.dto.DemoItemCreateRequest;
import com.nz.admin.modules.demo.mapper.DemoItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 示例条目服务测试。
 */
class DemoItemServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DemoItemServiceImpl demoItemService;

    @Mock
    private DemoItemMapper demoItemMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(demoItemService, "baseMapper", demoItemMapper);
    }

    @Test
    void createShouldMapAndInsertItem() {
        when(demoItemMapper.insert(any(DemoItemDO.class))).thenAnswer(invocation -> {
            DemoItemDO item = invocation.getArgument(0);
            item.setId(12L);
            return 1;
        });

        DemoItemCreateRequest request = new DemoItemCreateRequest();
        request.setName("模块化示例");
        request.setCategory("architecture");
        request.setStatus(0);
        request.setSort(10);
        request.setRemark("可独立删除");

        Long id = demoItemService.create(request);

        ArgumentCaptor<DemoItemDO> captor = ArgumentCaptor.forClass(DemoItemDO.class);
        verify(demoItemMapper).insert(captor.capture());
        assertEquals(12L, id);
        assertEquals("模块化示例", captor.getValue().getName());
        assertEquals("architecture", captor.getValue().getCategory());
    }

    @Test
    void getRequiredShouldRejectMissingItem() {
        when(demoItemMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> demoItemService.getRequired(99L));
    }

    @Test
    void deleteShouldStopWhenItemDoesNotExist() {
        when(demoItemMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> demoItemService.delete(99L));
        verify(demoItemMapper, never()).deleteById(99L);
    }
}
