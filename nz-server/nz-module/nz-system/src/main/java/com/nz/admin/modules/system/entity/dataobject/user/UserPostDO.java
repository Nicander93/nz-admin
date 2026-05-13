package com.nz.admin.modules.system.entity.dataobject.user;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("sys_user_post")
public class UserPostDO {

    private Long userId;
    private Long postId;
}
