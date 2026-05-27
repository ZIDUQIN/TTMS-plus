package com.ttms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ttms.dto.ApiResponse;
import com.ttms.entity.SystemConfig;
import com.ttms.entity.User;
import com.ttms.mapper.OrderLogMapper;
import com.ttms.mapper.SystemConfigMapper;
import com.ttms.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统控制器
 * 处理系统配置管理、主题偏好设置、操作日志查询
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SystemController {

    private final SystemConfigMapper systemConfigMapper;
    private final UserMapper userMapper;
    private final OrderLogMapper orderLogMapper;

    // ==================== 管理端接口 ====================

    /**
     * 获取系统配置
     * GET /api/admin/system/config
     *
     * @return 系统配置键值对Map
     */
    @GetMapping("/api/admin/system/config")
    public ApiResponse<Map<String, String>> getConfig() {
        log.debug("获取系统配置");
        List<SystemConfig> configs = systemConfigMapper.selectList(null);
        Map<String, String> configMap = new HashMap<>();
        for (SystemConfig config : configs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }
        return ApiResponse.success(configMap);
    }

    /**
     * 更新系统配置
     * PUT /api/admin/system/config
     *
     * @param configMap 配置键值对Map
     * @return 操作结果
     */
    @PutMapping("/api/admin/system/config")
    public ApiResponse<Void> updateConfig(@RequestBody Map<String, String> configMap) {
        log.info("更新系统配置: {}", configMap.keySet());
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            SystemConfig config = systemConfigMapper.selectByKey(entry.getKey());
            if (config != null) {
                config.setConfigValue(entry.getValue());
                systemConfigMapper.updateById(config);
            } else {
                // 如果配置项不存在，则新建
                SystemConfig newConfig = new SystemConfig();
                newConfig.setConfigKey(entry.getKey());
                newConfig.setConfigValue(entry.getValue());
                newConfig.setDescription("动态添加的配置项");
                systemConfigMapper.insert(newConfig);
            }
        }
        return ApiResponse.success("系统配置更新成功");
    }

    /**
     * 获取操作日志列表
     * GET /api/admin/system/logs
     *
     * @return 操作日志列表
     */
    @GetMapping("/api/admin/system/logs")
    public ApiResponse<List<?>> getLogs() {
        log.debug("查询操作日志");
        // 从order_log表获取所有日志
        return ApiResponse.success(orderLogMapper.selectAllLogs());
    }

    // ==================== 用户端接口 ====================

    /**
     * 保存用户主题偏好
     * POST /api/user/theme
     *
     * @param params 包含theme字段的Map
     * @return 操作结果
     */
    @PostMapping("/api/user/theme")
    public ApiResponse<Void> saveTheme(@RequestBody Map<String, String> params) {
        String theme = params.get("theme");
        if (theme == null || theme.isBlank()) {
            return ApiResponse.badRequest("主题不能为空");
        }

        Long userId = getCurrentUserId();
        log.info("保存用户主题: userId={}, theme={}", userId, theme);

        User user = userMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        user.setTheme(theme);
        userMapper.updateById(user);
        log.info("主题保存成功: userId={}, theme={}", userId, theme);
        return ApiResponse.success("主题保存成功");
    }

    /**
     * 获取用户主题偏好
     * GET /api/user/theme
     *
     * @return 当前用户的主题设置
     */
    @GetMapping("/api/user/theme")
    public ApiResponse<Map<String, String>> getTheme() {
        Long userId = getCurrentUserId();
        User user = userMapper.selectById(userId);
        String theme = user != null && user.getTheme() != null ? user.getTheme() : "white";
        Map<String, String> result = new HashMap<>();
        result.put("theme", theme);
        return ApiResponse.success(result);
    }

    /**
     * 获取当前登录用户的ID
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
