package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 员工表 - 管理员/售票员
 */
@Data
@TableName("employee")
public class Employee {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工号 */
    private String employeeNo;

    /** 用户名 */
    private String username;

    /** 密码(BCrypt加密) - 仅写入，序列化时隐藏防止泄露 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 角色ID */
    private Long roleId;

    /** 账号状态: 0-正常 1-禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /* ===== 非数据库字段 ===== */
    @TableField(exist = false)
    private String roleName;

    @TableField(exist = false)
    private String roleCode;
}
