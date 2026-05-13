package com.nz.admin.modules.system.entity.vo.user;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户返回对象。
 */
@Data
@Accessors(chain = true)
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Long deptId;
    private Integer status;
    private List<Long> postIds;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

