package com.nz.admin.modules.system.entity.query.user;


import com.nz.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQuery extends PageQuery {

    private String username;
    private String nickname;
    private Integer status;
    private Boolean revealContacts;

}
