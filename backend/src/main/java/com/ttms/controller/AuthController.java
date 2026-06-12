package com.ttms.controller;

import com.ttms.dto.ApiResponse;
import com.ttms.dto.LoginRequest;
import com.ttms.dto.LoginResponse;
import com.ttms.dto.RegisterRequest;
import com.ttms.security.TokenBlacklist;
import com.ttms.security.JwtTokenProvider;
import com.ttms.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final TokenBlacklist tokenBlacklist;
    private final JwtTokenProvider jwtTokenProvider;

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
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("注册请求: username={}", request.getUsername());
        Long userId = authService.register(request);
        log.info("注册成功: username={}, userId={}", request.getUsername(), userId);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("userId", userId);
        return ApiResponse.success("注册成功", data);
    }

    /**
     * 修改密码
     * 需要认证后才能访问，验证原密码正确后才能修改
     * 修改成功后会将当前Token加入黑名单，强制重新登录
     * POST /api/auth/change-password
     *
     * @param params 包含oldPassword和newPassword的Map
     * @return 操作结果
     */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> params, HttpServletRequest request) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (oldPassword == null || oldPassword.isBlank()) {
            return ApiResponse.badRequest("原密码不能为空");
        }
        if (newPassword == null || newPassword.isBlank()) {
            return ApiResponse.badRequest("新密码不能为空");
        }
        if (newPassword.length() < 8) {
            return ApiResponse.badRequest("新密码长度至少8位，需包含字母和数字");
        }
        if (!newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*[0-9].*")) {
            return ApiResponse.badRequest("新密码必须同时包含字母和数字");
        }

        Long userId = getCurrentUserId();
        log.info("修改密码请求: userId={}", userId);
        authService.changePassword(userId, oldPassword, newPassword);

        // 将当前Token加入黑名单，强制用户使用新密码重新登录
        blacklistCurrentToken(request);
        log.info("密码修改成功，Token已失效: userId={}", userId);
        return ApiResponse.success("密码修改成功，请重新登录");
    }

    /**
     * 用户/员工登出
     * POST /api/auth/logout
     * 将当前Token加入黑名单使其失效
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        blacklistCurrentToken(request);
        SecurityContextHolder.clearContext();
        log.info("用户已登出");
        return ApiResponse.success("已安全退出");
    }

    /**
     * 将当前请求的Token加入黑名单
     */
    private void blacklistCurrentToken(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            long exp = jwtTokenProvider.getExpiration(token);
            tokenBlacklist.blacklist(token, exp);
        }
    }

    /**
     * 从请求头提取Bearer Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /**
     * 从Spring Security上下文中获取当前登录用户的ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.ttms.exception.BusinessException(401, "请先登录");
        }
        return Long.valueOf(authentication.getPrincipal().toString());
    }

    /**
     * 获取客户端真实IP（支持代理/负载均衡场景）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For可能包含多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 日志脱敏：用户名只显示首字符
     */
    private String maskUsername(String username) {
        if (username == null || username.length() <= 1) return "***";
        return username.charAt(0) + "***";
    }
}
