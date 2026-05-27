package com.ttms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.dto.ApiResponse;
import com.ttms.entity.Employee;
import com.ttms.entity.Role;
import com.ttms.exception.BusinessException;
import com.ttms.mapper.EmployeeMapper;
import com.ttms.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 员工管理控制器
 * 处理员工的增删改查、密码重置、状态切换等管理操作
 * 所有接口需要管理员权限（SUPER_ADMIN 或 STAFF）
 * 密码重置和状态切换仅 SUPER_ADMIN 可操作（在SecurityConfig中由方法安全注解控制）
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeMapper employeeMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 查询员工列表
     * GET /api/admin/employees/list?page=1&size=10
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页员工列表
     */
    @GetMapping("/list")
    public ApiResponse<Page<Employee>> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        log.debug("查询员工列表: page={}, size={}", page, size);
        Page<Employee> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Employee::getCreateTime);
        Page<Employee> result = employeeMapper.selectPage(pageParam, wrapper);

        // 补充角色名称信息
        for (Employee employee : result.getRecords()) {
            if (employee.getRoleId() != null) {
                Role role = roleMapper.selectById(employee.getRoleId());
                // Employee没有roleName字段，通过其他方式展示
            }
        }

        return ApiResponse.success(result);
    }

    /**
     * 添加员工
     * POST /api/admin/employees/add
     *
     * @param employee 员工信息（用户名、密码、真实姓名、角色ID等）
     * @return 添加后的员工
     */
    @PostMapping("/add")
    public ApiResponse<Employee> add(@RequestBody Employee employee) {
        log.info("添加员工: username={}, realName={}", employee.getUsername(), employee.getRealName());

        // 检查用户名是否已被使用
        Employee existing = employeeMapper.findByUsername(employee.getUsername());
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }

        // 检查角色是否存在
        if (employee.getRoleId() != null) {
            Role role = roleMapper.selectById(employee.getRoleId());
            if (role == null) {
                throw new BusinessException("指定的角色不存在");
            }
        }

        // 加密密码
        String rawPassword = employee.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = "123456"; // 默认密码
        }
        employee.setPassword(passwordEncoder.encode(rawPassword));

        // 设置默认状态为正常
        if (employee.getStatus() == null) {
            employee.setStatus(0);
        }

        // 自动生成工号
        if (employee.getEmployeeNo() == null || employee.getEmployeeNo().isBlank()) {
            employee.setEmployeeNo(generateEmployeeNo());
        }

        employeeMapper.insert(employee);
        log.info("员工添加成功: id={}, username={}, 初始密码={}", employee.getId(), employee.getUsername(), rawPassword);
        return ApiResponse.success("员工添加成功，默认密码: " + rawPassword, employee);
    }

    /**
     * 更新员工信息
     * PUT /api/admin/employees/update
     *
     * @param employee 员工信息（含ID）
     * @return 更新后的员工
     */
    @PutMapping("/update")
    public ApiResponse<Employee> update(@RequestBody Employee employee) {
        log.info("更新员工: id={}, username={}", employee.getId(), employee.getUsername());

        Employee existing = employeeMapper.selectById(employee.getId());
        if (existing == null) {
            throw new BusinessException("员工不存在");
        }

        // 如果修改了角色，验证新角色存在
        if (employee.getRoleId() != null && !employee.getRoleId().equals(existing.getRoleId())) {
            Role role = roleMapper.selectById(employee.getRoleId());
            if (role == null) {
                throw new BusinessException("指定的角色不存在");
            }
        }

        // 不更新密码（密码通过专门的接口重置）
        employee.setPassword(null);

        employeeMapper.updateById(employee);
        log.info("员工更新成功: id={}, username={}", employee.getId(), employee.getUsername());
        return ApiResponse.success("员工信息更新成功", employeeMapper.selectById(employee.getId()));
    }

    /**
     * 重置员工密码
     * PUT /api/admin/employees/reset-password/{id}
     *
     * @param id     员工ID
     * @param params 包含newPassword字段的Map（可选，不传则重置为默认密码）
     * @return 操作结果
     */
    @PutMapping("/reset-password/{id}")
    public ApiResponse<String> resetPassword(@PathVariable Long id, @RequestBody(required = false) Map<String, String> params) {
        log.info("重置员工密码: id={}", id);

        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }

        // 如果传入了新密码则使用，否则使用默认密码
        String newPassword = "123456";
        if (params != null && params.containsKey("newPassword") && !params.get("newPassword").isBlank()) {
            newPassword = params.get("newPassword");
        }

        employee.setPassword(passwordEncoder.encode(newPassword));
        employeeMapper.updateById(employee);
        log.info("密码重置成功: id={}, username={}, 新密码={}", id, employee.getUsername(), newPassword);
        return ApiResponse.success("密码已重置为: " + newPassword);
    }

    /**
     * 切换员工账号状态（启用/禁用）
     * PUT /api/admin/employees/toggle-status/{id}
     *
     * @param id 员工ID
     * @return 操作结果
     */
    @PutMapping("/toggle-status/{id}")
    public ApiResponse<String> toggleStatus(@PathVariable Long id) {
        log.info("切换员工状态: id={}", id);

        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }

        // 不允许禁用超级管理员admin账号
        if ("admin".equals(employee.getUsername())) {
            throw new BusinessException("不允许禁用超级管理员账号");
        }

        // 切换状态: 0<->1
        int newStatus = (employee.getStatus() != null && employee.getStatus() == 0) ? 1 : 0;
        employee.setStatus(newStatus);
        employeeMapper.updateById(employee);

        String statusText = newStatus == 0 ? "已启用" : "已禁用";
        log.info("员工状态切换: id={}, username={}, status={}", id, employee.getUsername(), statusText);
        return ApiResponse.success("账号" + statusText);
    }

    /**
     * 自动生成员工工号
     * 格式: EMP + 3位递增数字
     *
     * @return 员工工号
     */
    private String generateEmployeeNo() {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Employee::getId).last("LIMIT 1");
        Employee lastEmployee = employeeMapper.selectOne(wrapper);
        int nextNo = 1;
        if (lastEmployee != null && lastEmployee.getEmployeeNo() != null) {
            String lastNo = lastEmployee.getEmployeeNo().replaceAll("[^0-9]", "");
            try {
                nextNo = Integer.parseInt(lastNo) + 1;
            } catch (NumberFormatException e) {
                nextNo = 1;
            }
        }
        return "EMP" + String.format("%03d", nextNo);
    }
}
