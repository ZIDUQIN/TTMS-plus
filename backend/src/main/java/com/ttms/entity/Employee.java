package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
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

    /** 密码(BCrypt加密) */
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
}
