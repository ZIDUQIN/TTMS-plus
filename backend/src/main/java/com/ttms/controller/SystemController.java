package com.ttms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.dto.ApiResponse;
import com.ttms.entity.*;
import com.ttms.mapper.*;
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
    private final EmployeeMapper employeeMapper;

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
     * 操作类型中文映射
     */
    private static final Map<String, String> OPERATION_TYPE_LABEL = Map.of(
        "CREATE", "创建订单",
        "PAY", "支付订单",
        "RESCHEDULE", "改签",
        "REFUND", "退票",
        "EXPIRE", "自动取消"
    );

    /**
     * 获取操作日志列表
     * GET /api/admin/system/logs
     * 将原始OrderLog转换为前端可读格式（解析操作人名称、操作描述等）
     *
     * @return 操作日志列表
     */
    @GetMapping("/api/admin/system/logs")
    public ApiResponse<List<Map<String, Object>>> getLogs() {
        log.debug("查询操作日志");
        List<OrderLog> rawLogs = orderLogMapper.selectList(
            new LambdaQueryWrapper<OrderLog>().orderByDesc(OrderLog::getCreateTime));

        // 收集所有操作人ID，按类型分组批量查询
        List<Long> userIds = rawLogs.stream()
            .filter(l -> "USER".equals(l.getOperatorType()))
            .map(OrderLog::getOperatorId).filter(id -> id != null).distinct()
            .collect(java.util.stream.Collectors.toList());
        List<Long> employeeIds = rawLogs.stream()
            .filter(l -> "EMPLOYEE".equals(l.getOperatorType()))
            .map(OrderLog::getOperatorId).filter(id -> id != null).distinct()
            .collect(java.util.stream.Collectors.toList());

        Map<Long, String> userNameMap = userIds.isEmpty() ? java.util.Collections.emptyMap()
            : userMapper.selectBatchIds(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getUsername));
        Map<Long, String> employeeNameMap = employeeIds.isEmpty() ? java.util.Collections.emptyMap()
            : employeeMapper.selectBatchIds(employeeIds).stream()
                .collect(java.util.stream.Collectors.toMap(Employee::getId, Employee::getRealName,
                    (e1, e2) -> e1));

        // 转换为前端可读格式
        List<Map<String, Object>> result = rawLogs.stream().map(l -> {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", l.getId());
            item.put("orderId", l.getOrderId());

            // 解析操作人名称
            String operatorName = resolveOperatorName(l.getOperatorType(), l.getOperatorId(),
                userNameMap, employeeNameMap);
            item.put("operator", operatorName);

            // 操作描述
            String typeLabel = OPERATION_TYPE_LABEL.getOrDefault(l.getOperationType(), l.getOperationType());
            item.put("action", typeLabel + " | " + (l.getAfterContent() != null ? l.getAfterContent() : ""));
            item.put("module", "订单管理");
            item.put("result", "成功");
            item.put("remark", l.getRemark());
            item.put("ip", "--");
            item.put("createTime", l.getCreateTime());
            return item;
        }).collect(java.util.stream.Collectors.toList());

        return ApiResponse.success(result);
    }

    /**
     * 解析操作人名称
     */
    private String resolveOperatorName(String operatorType, Long operatorId,
                                        Map<Long, String> userNameMap,
                                        Map<Long, String> employeeNameMap) {
        if (operatorType == null || operatorId == null) {
            return "--";
        }
        return switch (operatorType) {
            case "USER" -> userNameMap.getOrDefault(operatorId, "用户#" + operatorId);
            case "EMPLOYEE" -> employeeNameMap.getOrDefault(operatorId, "员工#" + operatorId);
            case "SYSTEM" -> "系统";
            default -> operatorType + "#" + operatorId;
        };
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
