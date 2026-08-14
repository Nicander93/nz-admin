package com.nz.admin.modules.system.mapper.sms;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.dataobject.sms.SmsTemplateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SmsTemplateMapper extends BaseMapper<SmsTemplateDO> {
}
