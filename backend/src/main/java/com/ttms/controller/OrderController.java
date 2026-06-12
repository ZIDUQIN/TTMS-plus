package com.ttms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.dto.ApiResponse;
import com.ttms.dto.OrderRequest;
import com.ttms.dto.RescheduleRequest;
import com.ttms.entity.Order;
import com.ttms.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端订单控制器
 * 处理普通用户的订单操作（创建、支付、改签、退票、查询）
 * 所有接口需要 USER 角色权限
 */
@Slf4j
@RestController
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单（用户选座下单）
     * POST /api/user/orders/create
     *
     * @param request 订单请求（场次ID、座位编号列表）
     * @return 创建的订单
     */
    @PostMapping("/create")
    public ApiResponse<Order> createOrder(@Valid @RequestBody OrderRequest request) {
        Long userId = getCurrentUserId();
        log.info("用户下单: userId={}, scheduleId={}, seats={}", userId, request.getScheduleId(), request.getSeatNumbers());
        Order order = orderService.createOrder(request, userId);
        log.info("下单成功: orderNo={}, 金额={}", order.getOrderNo(), order.getTotalPrice());
        return ApiResponse.success("下单成功，请尽快支付", order);
    }

    /**
     * 支付订单
     * POST /api/user/orders/pay/{orderId}
     *
     * @param orderId 订单ID
     * @param body    可选参数：userCouponId（使用的优惠券ID）
     * @return 支付后的订单
     */
    @PostMapping("/pay/{orderId}")
    public ApiResponse<Order> payOrder(@PathVariable Long orderId,
                                        @RequestBody(required = false) Map<String, Object> body) {
        Long userId = getCurrentUserId();
        Long userCouponId = null;
        if (body != null && body.get("userCouponId") != null) {
            userCouponId = Long.valueOf(body.get("userCouponId").toString());
        }
        log.info("支付订单: userId={}, orderId={}, userCouponId={}", userId, orderId, userCouponId);
        Order order = orderService.payOrder(orderId, userId, userCouponId);
        log.info("支付成功: orderNo={}", order.getOrderNo());
        return ApiResponse.success("支付成功", order);
    }

    /**
     * 查询我的订单
     * GET /api/user/orders/my?page=1&size=10
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页订单列表
     */
    @GetMapping("/my")
    public ApiResponse<Page<Order>> myOrders(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        log.debug("查询用户订单: userId={}, page={}, size={}", userId, page, size);
        Page<Order> orders = orderService.listByUser(userId, page, size);
        return ApiResponse.success(orders);
    }

    /**
     * 查询订单详情
     * GET /api/user/orders/detail/{id}
     *
     * @param id 订单ID
     * @return 订单详情
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<Order> detail(@PathVariable Long id) {
        log.debug("查询订单详情: id={}", id);
        Order order = orderService.detail(id);
        // 校验只能查看自己的订单
        Long userId = getCurrentUserId();
        if (!order.getUserId().equals(userId)) {
            return ApiResponse.forbidden("无权查看他人订单");
        }
        return ApiResponse.success(order);
    }

    /**
     * 改签
     * POST /api/user/orders/reschedule
     *
     * @param request 改签请求（原订单ID、新场次ID、新座位）
     * @return 改签后的新订单
     */
    @PostMapping("/reschedule")
    public ApiResponse<Order> reschedule(@Valid @RequestBody RescheduleRequest request) {
        Long userId = getCurrentUserId();
        log.info("改签请求: userId={}, 原订单={}, 新场次={}", userId, request.getOrderId(), request.getNewScheduleId());
        Order newOrder = orderService.reschedule(request, userId);
        log.info("改签成功: 新订单={}", newOrder.getOrderNo());
        return ApiResponse.success("改签成功", newOrder);
    }

    /**
     * 取消未支付订单
     * POST /api/user/orders/cancel/{orderId}
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @PostMapping("/cancel/{orderId}")
    public ApiResponse<Void> cancelOrder(@PathVariable Long orderId) {
        Long userId = getCurrentUserId();
        log.info("取消订单请求: userId={}, orderId={}", userId, orderId);
        orderService.cancelOrder(orderId, userId);
        log.info("取消订单成功: orderId={}", orderId);
        return ApiResponse.success("订单已取消，座位已释放");
    }

    /**
     * 退票
     * POST /api/user/orders/refund/{orderId}
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @PostMapping("/refund/{orderId}")
    public ApiResponse<Void> refund(@PathVariable Long orderId) {
        Long userId = getCurrentUserId();
        log.info("退票请求: userId={}, orderId={}", userId, orderId);
        orderService.refund(orderId, userId);
        log.info("退票成功: orderId={}", orderId);
        return ApiResponse.success("退票成功");
    }

    /**
     * 从Spring Security上下文中获取当前登录用户的ID
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
