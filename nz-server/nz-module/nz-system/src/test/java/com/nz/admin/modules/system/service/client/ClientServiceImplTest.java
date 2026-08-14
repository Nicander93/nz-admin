package com.nz.admin.modules.system.service.client;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.system.entity.dto.client.ClientCreateRequest;
import com.nz.admin.modules.system.entity.dataobject.client.ClientDO;
import com.nz.admin.modules.system.mapper.client.ClientMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客户端管理服务测试。
 */
class ClientServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ClientServiceImpl clientService;
    @Mock
    private ClientMapper clientMapper;

    @Test
    void createShouldRejectDuplicateClientId() {
        ReflectionTestUtils.setField(clientService, "baseMapper", clientMapper);
        when(clientMapper.selectCount(any())).thenReturn(1L);

        ClientCreateRequest request = new ClientCreateRequest();
        request.setClientId("web");
        request.setClientName("Web 管理端");
        request.setLoginType("account");
        request.setTokenTimeout(7200);
        request.setStatus(0);

        assertThrows(BusinessException.class, () -> clientService.create(request));
        verify(clientMapper, never()).insert(any(ClientDO.class));
    }

    @Test
    void getEnabledForLoginReturnsMatchingClient() {
        ReflectionTestUtils.setField(clientService, "baseMapper", clientMapper);
        ClientDO client = new ClientDO()
                .setClientId("nz-web-sms")
                .setLoginType("sms")
                .setTokenTimeout(7200)
                .setStatus(0);
        when(clientMapper.selectOne(any())).thenReturn(client);

        ClientDO result = clientService.getEnabledForLogin("nz-web-sms", "sms");

        assertThat(result).isSameAs(client);
    }

    @Test
    void getEnabledForLoginRejectsWrongGrantType() {
        ReflectionTestUtils.setField(clientService, "baseMapper", clientMapper);
        when(clientMapper.selectOne(any())).thenReturn(new ClientDO()
                .setClientId("nz-web-account")
                .setLoginType("account")
                .setTokenTimeout(7200)
                .setStatus(0));

        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> clientService.getEnabledForLogin("nz-web-account", "sms")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("客户端未启用或不支持当前登录方式");
    }
}
