package com.nz.admin.modules.system.service.sms;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.sms.core.SmsChannelConfig;
import com.nz.admin.framework.sms.core.SmsGateway;
import com.nz.admin.framework.sms.core.SmsMessage;
import com.nz.admin.framework.sms.core.SmsSendResult;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.system.entity.dataobject.sms.SmsChannelDO;
import com.nz.admin.modules.system.entity.dataobject.sms.SmsSendLogDO;
import com.nz.admin.modules.system.entity.dataobject.sms.SmsTemplateDO;
import com.nz.admin.modules.system.entity.dto.sms.SmsChannelSaveRequest;
import com.nz.admin.modules.system.entity.dto.sms.SmsTestSendRequest;
import com.nz.admin.modules.system.mapper.sms.SmsChannelMapper;
import com.nz.admin.modules.system.mapper.sms.SmsSendLogMapper;
import com.nz.admin.modules.system.mapper.sms.SmsTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsServiceImplTest extends BaseMockitoUnitTest {
    @Mock
    private SmsChannelMapper channelMapper;
    @Mock
    private SmsTemplateMapper templateMapper;
    @Mock
    private SmsSendLogMapper sendLogMapper;
    @Mock
    private SmsGateway smsGateway;
    private SmsServiceImpl service;

    @BeforeEach
    void setUpService() {
        service = new SmsServiceImpl(channelMapper, templateMapper, sendLogMapper, smsGateway);
    }

    @Test
    void rendersTemplateSendsAndPersistsSuccess() {
        when(templateMapper.selectById(2L)).thenReturn(template("验证码 {{code}}"));
        when(channelMapper.selectById(1L)).thenReturn(channel());
        when(sendLogMapper.insert(any(SmsSendLogDO.class))).thenAnswer(invocation -> {
            ((SmsSendLogDO) invocation.getArgument(0)).setId(99L);
            return 1;
        });
        when(smsGateway.send(any(), any())).thenReturn(SmsSendResult.accepted("provider-1"));
        SmsTestSendRequest request = request(Map.of("code", "123456"));

        Long logId = service.sendTest(request);

        assertThat(logId).isEqualTo(99L);
        ArgumentCaptor<SmsMessage> message = ArgumentCaptor.forClass(SmsMessage.class);
        verify(smsGateway).send(any(SmsChannelConfig.class), message.capture());
        assertThat(message.getValue().content()).isEqualTo("验证码 123456");
        ArgumentCaptor<SmsSendLogDO> updated = ArgumentCaptor.forClass(SmsSendLogDO.class);
        verify(sendLogMapper).updateById(updated.capture());
        assertThat(updated.getValue().getSendStatus()).isEqualTo("SUCCESS");
        assertThat(updated.getValue().getProviderMessageId()).isEqualTo("provider-1");
    }

    @Test
    void rejectsMissingTemplateParametersBeforeCreatingLog() {
        when(templateMapper.selectById(2L)).thenReturn(template("验证码 {{code}}"));
        when(channelMapper.selectById(1L)).thenReturn(channel());

        assertThatThrownBy(() -> service.sendTest(request(Map.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("code");
        verify(sendLogMapper, never()).insert(any(SmsSendLogDO.class));
        verify(smsGateway, never()).send(any(), any());
    }

    @Test
    void persistsProviderFailure() {
        when(templateMapper.selectById(2L)).thenReturn(template("通知"));
        when(channelMapper.selectById(1L)).thenReturn(channel());
        when(smsGateway.send(any(), any())).thenThrow(new IllegalStateException("provider down"));

        assertThatThrownBy(() -> service.sendTest(request(Map.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("provider down");
        ArgumentCaptor<SmsSendLogDO> updated = ArgumentCaptor.forClass(SmsSendLogDO.class);
        verify(sendLogMapper).updateById(updated.capture());
        assertThat(updated.getValue().getSendStatus()).isEqualTo("FAILED");
        assertThat(updated.getValue().getErrorMessage()).isEqualTo("provider down");
    }

    @Test
    void keepsExistingSecretWhenUpdateLeavesItBlank() {
        SmsChannelDO current = channel().setAccessKeySecret("existing-secret");
        when(channelMapper.selectById(1L)).thenReturn(current);
        when(channelMapper.selectCount(any())).thenReturn(0L);
        SmsChannelSaveRequest request = new SmsChannelSaveRequest();
        request.setId(1L);
        request.setChannelCode("local");
        request.setChannelName("本地");
        request.setProviderCode("log");
        request.setStatus(0);
        request.setAccessKeySecret("");

        service.updateChannel(request);

        ArgumentCaptor<SmsChannelDO> updated = ArgumentCaptor.forClass(SmsChannelDO.class);
        verify(channelMapper).updateById(updated.capture());
        assertThat(updated.getValue().getAccessKeySecret()).isEqualTo("existing-secret");
    }

    private SmsTestSendRequest request(Map<String, Object> parameters) {
        SmsTestSendRequest request = new SmsTestSendRequest();
        request.setTemplateId(2L);
        request.setPhoneNumber("13800138000");
        request.setParameters(parameters);
        return request;
    }

    private SmsChannelDO channel() {
        return new SmsChannelDO().setId(1L).setChannelCode("local")
                .setChannelName("本地").setProviderCode("log").setStatus(0);
    }

    private SmsTemplateDO template(String content) {
        return new SmsTemplateDO().setId(2L).setChannelId(1L)
                .setTemplateCode("code").setTemplateName("验证码")
                .setContent(content).setStatus(0);
    }
}
