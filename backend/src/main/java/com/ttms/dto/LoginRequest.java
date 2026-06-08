package com.ttms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 登录类型: USER-用户端 ADMIN-管理端（可选，不传则自动检测） */
    private String loginType = "";
}
