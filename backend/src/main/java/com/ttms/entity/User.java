package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户表 - 普通顾客
 */
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码(BCrypt加密) - 仅写入，序列化时隐藏防止泄露 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 昵称 */
    private String nickname;

    /** 真实姓名 (B12实名制购票) */
    private String realName;

    /** 身份证号 (B12实名制购票) */
    private String idCard;

    /** 头像URL */
    private String avatar;

    /** 账号状态: 0-正常 1-禁用 */
    private Integer status;

    /** 用户偏好主题 */
    private String theme;

    /** 会员等级ID (B15会员体系) */
    private Long memberLevelId;

    /** 积分 (B15会员体系) */
    private Integer points;

    /** 储值余额 (B15会员体系) */
    private java.math.BigDecimal balance;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
