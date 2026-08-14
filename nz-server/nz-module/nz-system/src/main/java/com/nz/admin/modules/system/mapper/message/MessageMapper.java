package com.nz.admin.modules.system.mapper.message;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.message.MessageDO;
import com.nz.admin.modules.system.entity.query.message.MessageQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<MessageDO> {

    default Page<MessageDO> selectInbox(Page<MessageDO> page, Long userId, MessageQuery query) {
        return selectPage(page, new LambdaQueryWrapper<MessageDO>()
                .eq(MessageDO::getUserId, userId)
                .eq(StrUtil.isNotBlank(query.getCategory()),
                        MessageDO::getCategory, query.getCategory())
                .eq(query.getReadStatus() != null,
                        MessageDO::getReadStatus, query.getReadStatus())
                .like(StrUtil.isNotBlank(query.getTitle()),
                        MessageDO::getTitle, query.getTitle())
                .orderByAsc(MessageDO::getReadStatus)
                .orderByDesc(MessageDO::getCreateTime)
                .orderByDesc(MessageDO::getId));
    }

    default Long selectUnreadCount(Long userId) {
        return selectCount(new LambdaQueryWrapper<MessageDO>()
                .eq(MessageDO::getUserId, userId)
                .eq(MessageDO::getReadStatus, 0));
    }
}
