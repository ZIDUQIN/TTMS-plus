package com.ttms.service.impl;

import com.ttms.dto.LoginRequest;
import com.ttms.dto.LoginResponse;
import com.ttms.dto.RegisterRequest;
import com.ttms.entity.Employee;
import com.ttms.entity.Role;
import com.ttms.entity.User;
import com.ttms.exception.BusinessException;
import com.ttms.mapper.EmployeeMapper;
import com.ttms.mapper.RoleMapper;
import com.ttms.mapper.UserMapper;
import com.ttms.security.JwtTokenProvider;
import com.ttms.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 认证服务实现类
 * 处理用户/员工登录、注册、密码修改等认证业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final EmployeeMapper employeeMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户/员工登录
     * 根据loginType区分用户端登录和管理端登录
     * 用户端查询user表，管理端查询employee表
     *
     * @param request 登录请求
     * @return 登录响应（令牌、用户信息、角色权限）
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        String loginType = request.getLoginType();

        // 管理端登录：查询employee表
        if ("ADMIN".equalsIgnoreCase(loginType)) {
            Employee employee = employeeMapper.findByUsername(username);
            if (employee == null) {
                throw new BusinessException(401, "用户名或密码错误");
            }
            // 检查账号是否已被禁用
            if (employee.getStatus() != null && employee.getStatus() == 1) {
                throw new BusinessException(403, "账号已被禁用，请联系管理员");
            }
            // 验证密码
            if (!passwordEncoder.matches(password, employee.getPassword())) {
                throw new BusinessException(401, "用户名或密码错误");
            }
            // 获取角色信息
            Role role = roleMapper.selectById(employee.getRoleId());
            if (role == null) {
                throw new BusinessException("用户角色未配置，请联系管理员");
            }
            // 生成JWT令牌
            String token = jwtTokenProvider.generateToken(
                employee.getId(), employee.getUsername(), role.getRoleCode(), "ADMIN");

            log.info("管理端登录成功: 用户名={}, 角色={}", employee.getUsername(), role.getRoleName());

            return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(employee.getId())
                .username(employee.getUsername())
                .realName(employee.getRealName())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .permissions(parsePermissions(role.getPermissions()))
                .build();

        // 用户端登录：查询user表
        } else {
            User user = userMapper.findByUsername(username);
            if (user == null) {
                throw new BusinessException(401, "用户名或密码错误");
            }
            // 检查账号是否已被禁用
            if (user.getStatus() != null && user.getStatus() == 1) {
                throw new BusinessException(403, "账号已被禁用，请联系管理员");
            }
            // 验证密码
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new BusinessException(401, "用户名或密码错误");
            }
            // 生成JWT令牌（普通用户固定ROLE_USER角色）
            String token = jwtTokenProvider.generateToken(
                user.getId(), user.getUsername(), "ROLE_USER", "USER");

            log.info("用户端登录成功: 用户名={}", user.getUsername());

            return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getNickname() != null ? user.getNickname() : user.getUsername())
                .roleCode("ROLE_USER")
                .roleName("普通用户")
                .theme(user.getTheme())
                .permissions(List.of("movie:view", "order:create", "order:view",
                    "order:reschedule", "order:refund", "theme:set"))
                .build();
        }
    }

    /**
     * 用户注册
     * 检查用户名唯一性，使用BCrypt加密密码，保存用户
     *
     * @param request 注册请求
     */
    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 检查用户名是否已被占用
        User existingUser = userMapper.findByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException("用户名已存在，请更换用户名");
        }
        // 检查员工表是否也有同名用户（防止用户名混淆）
        Employee existingEmployee = employeeMapper.findByUsername(request.getUsername());
        if (existingEmployee != null) {
            throw new BusinessException("用户名已存在，请更换用户名");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setStatus(0);    // 默认正常状态
        user.setTheme("white"); // 默认白色主题

        userMapper.insert(user);
        log.info("用户注册成功: 用户名={}", user.getUsername());
    }

    /**
     * 修改密码
     * 原密码验证通过后才能修改为新密码
     *
     * @param userId      用户ID
     * @param oldPassword 原密码
     * @param newPassword 新密码
     */
    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 验证原密码是否正确
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        // 校验新旧密码不能相同
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }
        // 加密新密码并更新
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        log.info("密码修改成功: userId={}", userId);
    }

    /**
     * 将JSON格式的权限字符串转换为List
     * 数据库存储格式: ["perm1","perm2"]
     *
     * @param permissionsJson JSON权限字符串
     * @return 权限列表
     */
    private List<String> parsePermissions(String permissionsJson) {
        // 简单解析JSON数组格式: ["a","b","c"]
        if (permissionsJson == null || permissionsJson.isBlank()) {
            return List.of();
        }
        String trimmed = permissionsJson.trim();
        // 去掉开头"[和结尾]"
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        // 按逗号分割，去掉引号
        String[] parts = trimmed.split(",");
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        for (String part : parts) {
            String cleaned = part.trim().replaceAll("^\"|\"$", "");
            if (!cleaned.isEmpty()) {
                result.add(cleaned);
            }
        }
        return result;
    }
}
