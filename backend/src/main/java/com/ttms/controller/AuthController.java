package com.ttms.controller;

import com.ttms.dto.ApiResponse;
import com.ttms.dto.LoginRequest;
import com.ttms.dto.LoginResponse;
import com.ttms.dto.RegisterRequest;
import com.ttms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * 处理用户登录、注册、修改密码等认证相关请求
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户/员工登录
     * 支持用户端(loginType=USER)和管理端(loginType=ADMIN)两种登录方式
     * POST /api/auth/login
     *
     * @param request 登录请求（用户名、密码、登录类型）
     * @return 登录响应（含JWT令牌和用户信息）
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("登录请求: username={}, loginType={}", request.getUsername(), request.getLoginType());
        LoginResponse response = authService.login(request);
        log.info("登录成功: username={}, role={}", response.getUsername(), response.getRoleName());
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 用户注册
     * 仅支持普通用户注册，管理员需由超级管理员在后台创建
     * POST /api/auth/register
     *
     * @param request 注册请求（用户名、密码等）
     * @return 操作结果
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        log.info("注册请求: username={}", request.getUsername());
        authService.register(request);
        log.info("注册成功: username={}", request.getUsername());
        return ApiResponse.success("注册成功，请登录");
    }

    /**
     * 修改密码
     * 需要认证后才能访问，验证原密码正确后才能修改
     * POST /api/auth/change-password
     *
     * @param params 包含oldPassword和newPassword的Map
     * @return 操作结果
     */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (oldPassword == null || oldPassword.isBlank()) {
            return ApiResponse.badRequest("原密码不能为空");
        }
        if (newPassword == null || newPassword.isBlank()) {
            return ApiResponse.badRequest("新密码不能为空");
        }
        if (newPassword.length() < 6) {
            return ApiResponse.badRequest("新密码长度至少6位");
        }

        // 从SecurityContext获取当前登录用户ID
        Long userId = getCurrentUserId();
        log.info("修改密码请求: userId={}", userId);
        authService.changePassword(userId, oldPassword, newPassword);
        log.info("密码修改成功: userId={}", userId);
        return ApiResponse.success("密码修改成功");
    }

    /**
     * 从Spring Security上下文中获取当前登录用户的ID
     * SecurityContextHolder中存储的是JwtAuthenticationFilter设置的认证信息
     * Principal就是userId的字符串形式
     *
     * @return 当前用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未登录");
        }
        return Long.valueOf(authentication.getPrincipal().toString());
    }
}
