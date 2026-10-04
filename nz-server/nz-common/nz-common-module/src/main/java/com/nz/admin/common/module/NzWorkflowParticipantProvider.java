package com.nz.admin.common.module;

import java.util.List;

/** 设计器办理人选择器；实现者按当前可信租户查询有效用户和角色。 */
public interface NzWorkflowParticipantProvider {
    Selection select(String type, String code, String name, int page, int size);
    List<Participant> feedback(List<String> storageIds);

    record Participant(String storageId, String handlerCode, String handlerName,
                       String groupName, String createTime) {}
    record Selection(List<Participant> list, long total) {}
}
