package com.nz.admin.modules.system.service.sms;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nz.admin.modules.system.entity.dto.sms.SmsChannelSaveRequest;
import com.nz.admin.modules.system.entity.dto.sms.SmsTemplateSaveRequest;
import com.nz.admin.modules.system.entity.dto.sms.SmsTestSendRequest;
import com.nz.admin.modules.system.entity.vo.sms.SmsChannelVO;
import com.nz.admin.modules.system.entity.vo.sms.SmsSendLogVO;
import com.nz.admin.modules.system.entity.vo.sms.SmsTemplateVO;
import java.util.Map;


/** 短信管理服务。 */
public interface SmsService {
    IPage<SmsChannelVO> pageChannels(Integer pageNum, Integer pageSize, String keyword, Integer status);
    SmsChannelVO getChannel(Long id);
    Long createChannel(SmsChannelSaveRequest request);
    void updateChannel(SmsChannelSaveRequest request);
    void deleteChannel(Long id);
    IPage<SmsTemplateVO> pageTemplates(Integer pageNum, Integer pageSize, Long channelId,
                                        String keyword, Integer status);
    SmsTemplateVO getTemplate(Long id);
    Long createTemplate(SmsTemplateSaveRequest request);
    void updateTemplate(SmsTemplateSaveRequest request);
    void deleteTemplate(Long id);
    Long sendByTemplateCode(String phoneNumber, String templateCode, Map<String, Object> parameters);
    IPage<SmsSendLogVO> pageLogs(Integer pageNum, Integer pageSize, String sendStatus);
    Long sendTest(SmsTestSendRequest request);
}
