package com.nz.admin.modules.system.service.sms;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.encryption.mask.SensitiveDataUtils;
import com.nz.admin.framework.sms.core.SmsChannelConfig;
import com.nz.admin.framework.sms.core.SmsGateway;
import com.nz.admin.framework.sms.core.SmsMessage;
import com.nz.admin.framework.sms.core.SmsSendResult;
import com.nz.admin.modules.system.entity.dataobject.sms.SmsChannelDO;
import com.nz.admin.modules.system.entity.dataobject.sms.SmsSendLogDO;
import com.nz.admin.modules.system.entity.dataobject.sms.SmsTemplateDO;
import com.nz.admin.modules.system.entity.dto.sms.SmsChannelSaveRequest;
import com.nz.admin.modules.system.entity.dto.sms.SmsTemplateSaveRequest;
import com.nz.admin.modules.system.entity.dto.sms.SmsTestSendRequest;
import com.nz.admin.modules.system.entity.vo.sms.SmsChannelVO;
import com.nz.admin.modules.system.entity.vo.sms.SmsSendLogVO;
import com.nz.admin.modules.system.entity.vo.sms.SmsTemplateVO;
import com.nz.admin.modules.system.mapper.sms.SmsChannelMapper;
import com.nz.admin.modules.system.mapper.sms.SmsSendLogMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import com.nz.admin.modules.system.mapper.sms.SmsTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 短信管理服务实现。 */
@ConditionalOnBean(SmsGateway.class)
@Service
public class SmsServiceImpl implements SmsService {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");
    private static final Set<Integer> VALID_STATUSES = Set.of(0, 1);
    private final SmsChannelMapper channelMapper;
    private final SmsTemplateMapper templateMapper;
    private final SmsSendLogMapper sendLogMapper;
    private final SmsGateway smsGateway;

    public SmsServiceImpl(SmsChannelMapper channelMapper, SmsTemplateMapper templateMapper,
                          SmsSendLogMapper sendLogMapper, SmsGateway smsGateway) {
        this.channelMapper = channelMapper;
        this.templateMapper = templateMapper;
        this.sendLogMapper = sendLogMapper;
        this.smsGateway = smsGateway;
    }

    @Override
    public IPage<SmsChannelVO> pageChannels(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        return channelMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SmsChannelDO>()
                        .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                                .like(SmsChannelDO::getChannelCode, keyword)
                                .or().like(SmsChannelDO::getChannelName, keyword))
                        .eq(status != null, SmsChannelDO::getStatus, status)
                        .orderByDesc(SmsChannelDO::getId))
                .convert(this::toChannelVO);
    }

    @Override
    public SmsChannelVO getChannel(Long id) {
        return toChannelVO(getRequiredChannel(id));
    }

    @Override
    public Long createChannel(SmsChannelSaveRequest request) {
        validateChannelRequest(request);
        ensureChannelCodeUnique(request.getChannelCode(), null);
        SmsChannelDO channel = BeanUtil.copyProperties(request, SmsChannelDO.class);
        channel.setId(null);
        channelMapper.insert(channel);
        return channel.getId();
    }

    @Override
    public void updateChannel(SmsChannelSaveRequest request) {
        if (request.getId() == null) {
            throw new BusinessException("短信渠道 ID 不能为空");
        }
        SmsChannelDO current = getRequiredChannel(request.getId());
        validateChannelRequest(request);
        ensureChannelCodeUnique(request.getChannelCode(), request.getId());
        SmsChannelDO update = BeanUtil.copyProperties(request, SmsChannelDO.class);
        if (StrUtil.isBlank(request.getAccessKeySecret())) {
            update.setAccessKeySecret(current.getAccessKeySecret());
        }
        channelMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChannel(Long id) {
        getRequiredChannel(id);
        Long templateCount = templateMapper.selectCount(new LambdaQueryWrapper<SmsTemplateDO>()
                .eq(SmsTemplateDO::getChannelId, id));
        if (templateCount != null && templateCount > 0) {
            throw new BusinessException("渠道下仍有短信模板，不能删除");
        }
        channelMapper.deleteById(id);
    }

    @Override
    public IPage<SmsTemplateVO> pageTemplates(Integer pageNum, Integer pageSize, Long channelId,
                                               String keyword, Integer status) {
        Page<SmsTemplateDO> page = templateMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SmsTemplateDO>()
                        .eq(channelId != null, SmsTemplateDO::getChannelId, channelId)
                        .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                                .like(SmsTemplateDO::getTemplateCode, keyword)
                                .or().like(SmsTemplateDO::getTemplateName, keyword))
                        .eq(status != null, SmsTemplateDO::getStatus, status)
                        .orderByDesc(SmsTemplateDO::getId));
        Map<Long, SmsChannelDO> channels = loadChannels(page.getRecords().stream()
                .map(SmsTemplateDO::getChannelId).collect(Collectors.toSet()));
        return page.convert(template -> toTemplateVO(template, channels.get(template.getChannelId())));
    }

    @Override
    public SmsTemplateVO getTemplate(Long id) {
        SmsTemplateDO template = getRequiredTemplate(id);
        return toTemplateVO(template, getRequiredChannel(template.getChannelId()));
    }

    @Override
    public Long createTemplate(SmsTemplateSaveRequest request) {
        validateTemplateRequest(request);
        ensureTemplateCodeUnique(request.getChannelId(), request.getTemplateCode(), null);
        SmsTemplateDO template = BeanUtil.copyProperties(request, SmsTemplateDO.class);
        template.setId(null);
        templateMapper.insert(template);
        return template.getId();
    }

    @Override
    public void updateTemplate(SmsTemplateSaveRequest request) {
        if (request.getId() == null) {
            throw new BusinessException("短信模板 ID 不能为空");
        }
        getRequiredTemplate(request.getId());
        validateTemplateRequest(request);
        ensureTemplateCodeUnique(request.getChannelId(), request.getTemplateCode(), request.getId());
        templateMapper.updateById(BeanUtil.copyProperties(request, SmsTemplateDO.class));
    }

    @Override
    public void deleteTemplate(Long id) {
        getRequiredTemplate(id);
        templateMapper.deleteById(id);
    }

    @Override
    public IPage<SmsSendLogVO> pageLogs(Integer pageNum, Integer pageSize, String sendStatus) {
        return sendLogMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SmsSendLogDO>()
                        .eq(StrUtil.isNotBlank(sendStatus), SmsSendLogDO::getSendStatus, sendStatus)
                        .orderByDesc(SmsSendLogDO::getId))
                .convert(this::toLogVO);
    }

    @Override
    public Long sendTest(SmsTestSendRequest request) {
        SmsTemplateDO template = getRequiredTemplate(request.getTemplateId());
        SmsChannelDO channel = getRequiredChannel(template.getChannelId());
        if (!Integer.valueOf(0).equals(template.getStatus()) || !Integer.valueOf(0).equals(channel.getStatus())) {
            throw new BusinessException("短信渠道和模板必须处于启用状态");
        }
        Map<String, Object> parameters = request.getParameters() == null
                ? Map.of() : new LinkedHashMap<>(request.getParameters());
        return send(template, channel, request.getPhoneNumber(), parameters, false);
    }

    @Override
    public Long sendByTemplateCode(String phoneNumber, String templateCode, Map<String, Object> parameters) {
        SmsTemplateDO template = templateMapper.selectList(new LambdaQueryWrapper<SmsTemplateDO>()
                        .eq(SmsTemplateDO::getTemplateCode, templateCode)
                        .eq(SmsTemplateDO::getStatus, 0)
                        .orderByAsc(SmsTemplateDO::getId)
                        .last("LIMIT 1"))
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException("可用的短信模板不存在: " + templateCode));
        SmsChannelDO channel = getRequiredChannel(template.getChannelId());
        if (!Integer.valueOf(0).equals(channel.getStatus())) {
            throw new BusinessException("短信渠道已停用");
        }
        return send(template, channel, phoneNumber,
                parameters == null ? Map.of() : new LinkedHashMap<>(parameters), true);
    }

    private Long send(SmsTemplateDO template, SmsChannelDO channel, String phoneNumber,
                      Map<String, Object> parameters, boolean sensitive) {
        String content = render(template.getContent(), parameters);
        SmsSendLogDO log = new SmsSendLogDO()
                .setChannelId(channel.getId()).setTemplateId(template.getId())
                .setPhoneNumber(phoneNumber).setTemplateCode(template.getTemplateCode())
                .setContent(sensitive ? "[敏感短信内容已隐藏]" : content)
                .setRequestParams(sensitive ? "{}" : JSONUtil.toJsonStr(parameters)).setSendStatus("PENDING");
        sendLogMapper.insert(log);
        try {
            SmsSendResult result = smsGateway.send(toChannelConfig(channel),
                    new SmsMessage(phoneNumber, providerTemplateCode(template), content, parameters));
            log.setSendStatus("SUCCESS").setProviderMessageId(result.providerMessageId())
                    .setSendTime(LocalDateTime.now());
            sendLogMapper.updateById(log);
            return log.getId();
        } catch (RuntimeException exception) {
            log.setSendStatus("FAILED")
                    .setErrorMessage(StrUtil.subPre(StrUtil.blankToDefault(exception.getMessage(), "未知错误"), 500))
                    .setSendTime(LocalDateTime.now());
            sendLogMapper.updateById(log);
            throw new BusinessException("短信发送失败: " + log.getErrorMessage());
        }
    }

    private void validateChannelRequest(SmsChannelSaveRequest request) {
        validateStatus(request.getStatus());
        if ("webhook".equalsIgnoreCase(request.getProviderCode()) && StrUtil.isBlank(request.getEndpoint())) {
            throw new BusinessException("Webhook 渠道必须配置 endpoint");
        }
    }

    private void validateTemplateRequest(SmsTemplateSaveRequest request) {
        validateStatus(request.getStatus());
        getRequiredChannel(request.getChannelId());
    }

    private void validateStatus(Integer status) {
        if (status == null || !VALID_STATUSES.contains(status)) {
            throw new BusinessException("状态只能为启用或停用");
        }
    }

    private void ensureChannelCodeUnique(String channelCode, Long excludedId) {
        Long count = channelMapper.selectCount(new LambdaQueryWrapper<SmsChannelDO>()
                .eq(SmsChannelDO::getChannelCode, channelCode)
                .ne(excludedId != null, SmsChannelDO::getId, excludedId));
        if (count != null && count > 0) {
            throw new BusinessException("短信渠道编码已存在");
        }
    }

    private void ensureTemplateCodeUnique(Long channelId, String templateCode, Long excludedId) {
        Long count = templateMapper.selectCount(new LambdaQueryWrapper<SmsTemplateDO>()
                .eq(SmsTemplateDO::getChannelId, channelId)
                .eq(SmsTemplateDO::getTemplateCode, templateCode)
                .ne(excludedId != null, SmsTemplateDO::getId, excludedId));
        if (count != null && count > 0) {
            throw new BusinessException("该渠道下的模板编码已存在");
        }
    }

    private SmsChannelDO getRequiredChannel(Long id) {
        SmsChannelDO channel = channelMapper.selectById(id);
        if (channel == null) {
            throw new BusinessException("短信渠道不存在");
        }
        return channel;
    }

    private SmsTemplateDO getRequiredTemplate(Long id) {
        SmsTemplateDO template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("短信模板不存在");
        }
        return template;
    }

    private Map<Long, SmsChannelDO> loadChannels(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return channelMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SmsChannelDO::getId, Function.identity()));
    }

    private String render(String source, Map<String, Object> parameters) {
        Matcher matcher = VARIABLE_PATTERN.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = parameters.get(key);
            if (value == null) {
                throw new BusinessException("短信模板缺少参数: " + key);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String providerTemplateCode(SmsTemplateDO template) {
        return StrUtil.blankToDefault(template.getProviderTemplateId(), template.getTemplateCode());
    }

    private SmsChannelConfig toChannelConfig(SmsChannelDO channel) {
        return new SmsChannelConfig(channel.getChannelCode(), channel.getProviderCode(), channel.getEndpoint(),
                channel.getAccessKeyId(), channel.getAccessKeySecret(), channel.getSignature());
    }

    private SmsChannelVO toChannelVO(SmsChannelDO channel) {
        SmsChannelVO vo = BeanUtil.copyProperties(channel, SmsChannelVO.class);
        vo.setAccessKeySecretConfigured(StrUtil.isNotBlank(channel.getAccessKeySecret()));
        return vo;
    }

    private SmsTemplateVO toTemplateVO(SmsTemplateDO template, SmsChannelDO channel) {
        SmsTemplateVO vo = BeanUtil.copyProperties(template, SmsTemplateVO.class);
        vo.setChannelName(channel == null ? "已删除渠道" : channel.getChannelName());
        return vo;
    }

    private SmsSendLogVO toLogVO(SmsSendLogDO log) {
        SmsSendLogVO vo = BeanUtil.copyProperties(log, SmsSendLogVO.class);
        vo.setPhoneNumberMasked(SensitiveDataUtils.maskPhone(log.getPhoneNumber()));
        return vo;
    }
}
