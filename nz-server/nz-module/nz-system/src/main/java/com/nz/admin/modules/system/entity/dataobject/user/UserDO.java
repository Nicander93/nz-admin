package com.nz.admin.modules.system.entity.dataobject.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.framework.encryption.mybatis.EncryptedStringTypeHandler;

import java.util.List;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_user", autoResultMap = true)
public class UserDO extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String username;
    private String password;
    @JsonIgnore
    private String phoneHash;
    private String nickname;
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String email;
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String phone;
    private Long deptId;
    private Integer status;
    private String gender;
    private Long avatarFileId;

    @TableField(exist = false)
    private List<Long> postIds;
}
