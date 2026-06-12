package com.ttms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.dto.OrderRequest;
import com.ttms.dto.RescheduleRequest;
import com.ttms.entity.Order;

/**
 * 订单服务接口
 * 负责订单的创建、支付、改签、退票等核心业务
 */
public interface OrderService {

    /**
     * 创建订单（用户选座下单）
     * 锁定座位、生成订单号、设置初始状态为待支付
     *
     * @param request 订单请求（场次ID、座位编号列表）
     * @param userId  下单用户ID
     * @return 创建的订单
     */
    Order createOrder(OrderRequest request, Long userId);

    /**
     * 支付订单
     * 将订单状态改为待观影，座位标记为已售出
     *
     * @param orderId      订单ID
     * @param userId       支付用户ID（用于权限校验）
     * @param userCouponId 使用的优惠券ID（可选，null表示不使用）
     * @return 支付后的订单
     */
    Order payOrder(Long orderId, Long userId, Long userCouponId);

    /**
     * 改签
     * 释放原座位、锁定新座位、创建新订单并关联原订单
     *
     * @param request 改签请求（原订单ID、新场次ID、新座位）
     * @param userId  用户ID
     * @return 改签后的新订单
     */
    Order reschedule(RescheduleRequest request, Long userId);

    /**
     * 退票
     * 取消订单、释放座位、记录操作日志
     *
     * @param orderId 订单ID
     * @param userId  退票用户ID
     */
    void refund(Long orderId, Long userId);

    /**
     * 查询用户的所有订单
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 分页订单列表
     */
    Page<Order> listByUser(Long userId, int page, int size);

    /**
     * 查询所有订单（管理端）
     *
     * @param page   页码
     * @param size   每页大小
     * @param status 状态筛选（null表示全部）
     * @return 分页订单列表
     */
    Page<Order> listAll(int page, int size, Integer status);

    /**
     * 查询订单详情
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    Order detail(Long orderId);

    /**
     * 取消过期未支付订单
     * 定时任务调用，释放超过15分钟未支付的订单的座位
     */
    void cancelExpired();

    /**
     * 协助创建订单（管理端替用户下单）
     *
     * @param request   订单请求
     * @param userId    目标用户ID
     * @param operatorId 操作员工ID
     * @return 创建的订单
     */
    Order assistCreate(OrderRequest request, Long userId, Long operatorId);

    /**
     * 协助支付订单（管理端替用户支付）
     *
     * @param orderId    订单ID
     * @param operatorId 操作员工ID
     * @return 支付后的订单
     */
    Order assistPay(Long orderId, Long operatorId);

    /**
     * 取消未支付订单
     * 释放座位、将订单状态改为已取消，无需先支付再退票
     *
     * @param orderId 订单ID
     * @param userId  用户ID
     */
    void cancelOrder(Long orderId, Long userId);

    /**
     * 协助退票（管理端替用户退票）
     * 释放座位、退回金额、记录操作日志，跳过用户所有权校验
     *
     * @param orderId    订单ID
     * @param operatorId 操作员工ID
     * @return 退票后的订单
     */
    Order assistRefund(Long orderId, Long operatorId);
}
