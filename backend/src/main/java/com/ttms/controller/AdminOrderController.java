package com.ttms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.dto.ApiResponse;
import com.ttms.dto.OrderRequest;
import com.ttms.entity.Order;
import com.ttms.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端订单控制器
 * 处理管理员/员工的订单操作（查看所有订单、协助创建订单）
 * 所有接口需要管理员权限
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * 查询所有订单列表
     * GET /api/admin/orders/list?page=1&size=10&status=1
     *
     * @param page   页码
     * @param size   每页大小
     * @param status 状态筛选（可选）
     * @return 分页订单列表
     */
    @GetMapping("/list")
    public ApiResponse<Page<Order>> list(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) Integer status) {
        log.debug("管理端查询订单列表: page={}, size={}, status={}", page, size, status);
        Page<Order> orders = orderService.listAll(page, size, status);
        return ApiResponse.success(orders);
    }

    /**
     * 查询订单详情
     * GET /api/admin/orders/detail/{id}
     *
     * @param id 订单ID
     * @return 订单详情
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<Order> detail(@PathVariable Long id) {
        log.debug("管理端查询订单详情: id={}", id);
        Order order = orderService.detail(id);
        return ApiResponse.success(order);
    }

    /**
     * 协助创建订单（管理员替顾客下单）
     * POST /api/admin/orders/assist-create
     *
     * @param request 订单请求（需额外包含userId字段表示目标用户）
     * @return 创建的订单
     */
    @PostMapping("/assist-create")
    public ApiResponse<Order> assistCreate(@Valid @RequestBody OrderRequest request) {
        Long operatorId = getCurrentUserId();
        Long targetUserId = request.getUserId() != null ? request.getUserId() : operatorId;
        log.info("管理端协助下单: operatorId={}, targetUserId={}, scheduleId={}, seats={}",
            operatorId, targetUserId, request.getScheduleId(), request.getSeatNumbers());
        Order order = orderService.assistCreate(request, targetUserId, operatorId);
        log.info("协助下单成功: orderNo={}", order.getOrderNo());
        return ApiResponse.success("协助下单成功", order);
    }

    /**
     * 协助创建订单（指定用户）
     * POST /api/admin/orders/assist-create/{userId}
     *
     * @param userId  目标用户ID
     * @param request 订单请求
     * @return 创建的订单
     */
    @PostMapping("/assist-create/{userId}")
    public ApiResponse<Order> assistCreateForUser(@PathVariable Long userId,
                                                   @Valid @RequestBody OrderRequest request) {
        Long operatorId = getCurrentUserId();
        log.info("管理端协助用户下单: operatorId={}, targetUserId={}, scheduleId={}",
            operatorId, userId, request.getScheduleId());
        Order order = orderService.assistCreate(request, userId, operatorId);
        log.info("协助下单成功: orderNo={}, 目标用户={}", order.getOrderNo(), userId);
        return ApiResponse.success("协助下单成功", order);
    }

    /**
     * 协助支付订单（管理端替用户支付）
     * POST /api/admin/orders/assist-pay/{orderId}
     *
     * @param orderId 订单ID
     * @return 支付后的订单
     */
    @PostMapping("/assist-pay/{orderId}")
    public ApiResponse<Order> assistPay(@PathVariable Long orderId) {
        Long operatorId = getCurrentUserId();
        log.info("管理端协助支付: operatorId={}, orderId={}", operatorId, orderId);
        Order order = orderService.assistPay(orderId, operatorId);
        log.info("协助支付成功: orderNo={}", order.getOrderNo());
        return ApiResponse.success("协助支付成功", order);
    }

    /**
     * 协助退票（管理端替用户退票）
     * POST /api/admin/orders/assist-refund/{orderId}
     *
     * @param orderId 订单ID
     * @return 退票后的订单
     */
    @PostMapping("/assist-refund/{orderId}")
    public ApiResponse<Order> assistRefund(@PathVariable Long orderId) {
        Long operatorId = getCurrentUserId();
        log.info("管理端协助退票: operatorId={}, orderId={}", operatorId, orderId);
        Order order = orderService.assistRefund(orderId, operatorId);
        log.info("协助退票成功: orderNo={}", order.getOrderNo());
        return ApiResponse.success("退票成功", order);
    }

    /**
     * 获取当前登录员工/管理员的ID
     *
     * @return 当前操作员ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未登录");
        }
        return Long.valueOf(authentication.getPrincipal().toString());
    }
}
