package com.ttms.service;

import com.ttms.dto.LoginRequest;
import com.ttms.dto.LoginResponse;
import com.ttms.dto.RegisterRequest;

/**
 * 认证服务接口
 * 负责用户/员工登录、注册、密码修改等认证相关业务
 */
public interface AuthService {

    /**
     * 用户/员工登录
     * 支持用户端（USER）和管理端（ADMIN）两种登录类型
     *
     * @param request 登录请求（用户名、密码、登录类型）
     * @return 登录响应（JWT令牌、用户信息、角色、权限列表）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户注册
     * 普通用户注册，默认赋予ROLE_USER角色
     *
     * @param request 注册请求（用户名、密码、手机号等信息）
     */
    Long register(RegisterRequest request);

    /**
     * 修改密码
     *
     * @param userId   用户ID
     * @param oldPassword 原密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
