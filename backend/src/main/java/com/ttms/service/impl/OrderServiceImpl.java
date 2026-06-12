package com.ttms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.dto.OrderRequest;
import com.ttms.dto.RescheduleRequest;
import com.ttms.entity.*;
import com.ttms.exception.BusinessException;
import com.ttms.mapper.*;
import com.ttms.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 * 负责订单的创建、支付、改签、退票等核心业务逻辑
 * 涉及事务管理、座位锁定/释放、订单状态机等关键逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderLogMapper orderLogMapper;
    private final ScheduleMapper scheduleMapper;
    private final SeatMapper seatMapper;
    private final MovieMapper movieMapper;
    private final HallMapper hallMapper;
    private final UserMapper userMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final MemberServiceImpl memberService;
    private final PricingServiceImpl pricingService;

    /** 随机字符集，用于生成订单号的随机部分 */
    private static final String RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    /**
     * 创建订单（用户选座下单）
     * 核心流程：
     * 1. 验证场次存在且未开始放映
     * 2. 验证请求的每个座位存在且状态为空闲
     * 3. 锁定所有座位（状态0->1）
     * 4. 生成唯一订单号
     * 5. 创建订单记录
     * 使用 @Transactional 确保原子性
     *
     * @param request 订单请求（场次ID、座位编号列表）
     * @param userId  下单用户ID
     * @return 创建的订单
     */
    @Override
    @Transactional
    public Order createOrder(OrderRequest request, Long userId) {
        // 1. 验证场次
        Schedule schedule = scheduleMapper.selectById(request.getScheduleId());
        if (schedule == null) {
            throw new BusinessException("场次不存在");
        }
        if (schedule.getStatus() != null && schedule.getStatus() != 1) {
            throw new BusinessException("该场次已取消或已结束，无法购票");
        }
        // 检查是否已经开始放映（开始时间在当前时间之前）
        if (schedule.getStartTime() != null && schedule.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("该场次已经开始放映，无法购票");
        }

        // 2. 验证并锁定座位
        List<String> seatNumbers = request.getSeatNumbers();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (String seatNumber : seatNumbers) {
            Seat seat = seatMapper.selectByScheduleAndNumber(request.getScheduleId(), seatNumber);
            if (seat == null) {
                throw new BusinessException("座位 " + seatNumber + " 不存在");
            }
            if (seat.getStatus() != 0) {
                throw new BusinessException("座位 " + seatNumber + " 已被占用，请重新选择");
            }
        }

        // 3. 生成订单号: yyyyMMdd + 8位随机字母数字
        String orderNo = generateOrderNo();

        // 4. 获取关联信息
        Movie movie = movieMapper.selectById(schedule.getMovieId());
        Hall hall = hallMapper.selectById(schedule.getHallId());

        // 5. 创建订单记录（先插入获取ID）
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setScheduleId(request.getScheduleId());
        order.setMovieId(schedule.getMovieId());
        order.setHallId(schedule.getHallId());
        order.setSeatNumbers(String.join(",", seatNumbers));
        order.setSeatCount(seatNumbers.size());
        order.setStatus(0);  // 待支付状态
        order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "WECHAT");
        orderMapper.insert(order);

        // 6. 锁定所有座位（需要订单ID）
        for (String seatNumber : seatNumbers) {
            Seat seat = seatMapper.selectByScheduleAndNumber(request.getScheduleId(), seatNumber);
            int locked = seatMapper.lockSeat(seat.getId(), order.getId());
            if (locked != 1) {
                // 并发情况下座位可能被其他请求抢占，回滚已锁定座位
                throw new BusinessException("座位 " + seatNumber + " 锁定失败，可能已被其他用户抢占");
            }
        }

        // 7. 计算总价（集成定价引擎：座位分区价+时段折扣+人群折扣+会员折扣）
        List<String> ticketTypes = request.getTicketTypes();
        totalPrice = pricingService.calculateOrderTotal(schedule, seatNumbers, ticketTypes);

        // B15: 应用会员折扣
        BigDecimal memberDiscount = memberService.getDiscountRate(userId);
        if (memberDiscount.compareTo(BigDecimal.ONE) < 0) {
            totalPrice = totalPrice.multiply(memberDiscount).setScale(2, java.math.RoundingMode.HALF_UP);
            log.info("会员折扣: userId={}, discountRate={}, finalPrice={}", userId, memberDiscount, totalPrice);
        }
        order.setTotalPrice(totalPrice);
        orderMapper.updateById(order);

        // 8. 原子增加场次已售数量（预占座位，防止竞态条件）
        scheduleMapper.incrementSoldCount(schedule.getId(), seatNumbers.size());

        // 9. 记录操作日志
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(order.getId());
        logEntry.setOperationType("CREATE");
        logEntry.setBeforeContent("");
        logEntry.setAfterContent("创建订单, 座位: " + order.getSeatNumbers());
        logEntry.setOperatorId(userId);
        logEntry.setOperatorType("USER");
        logEntry.setRemark("用户选座下单");
        orderLogMapper.insert(logEntry);

        log.info("订单创建成功: orderNo={}, userId={}, 座位={}, 金额={}",
            orderNo, userId, order.getSeatNumbers(), totalPrice);

        // 补充关联信息
        order.setMovieName(movie != null ? movie.getMovieName() : "");
        order.setMoviePoster(movie != null ? movie.getPosterUrl() : null);
        order.setHallName(hall != null ? hall.getHallName() : "");
        order.setStartTime(schedule.getStartTime());
        order.setEndTime(schedule.getEndTime());

        return order;
    }

    /**
     * 支付订单
     * 将订单状态从待支付(0)改为待观影(1)
     * 将座位状态从已锁定(1)改为已售出(2)
     * 记录支付时间
     *
     * @param orderId 订单ID
     * @param userId  支付用户ID（用于权限校验）
     * @return 支付后的订单
     */
    @Override
    @Transactional
    public Order payOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        // 权限校验：只能支付自己的订单，管理员协助支付通过独立接口
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作他人订单");
        }
        // 状态校验：只有待支付状态的订单才能支付
        if (order.getStatus() != 0) {
            if (order.getStatus() == 1) {
                throw new BusinessException("该订单已支付，请勿重复支付");
            } else if (order.getStatus() == 3) {
                throw new BusinessException("该订单已改签");
            } else if (order.getStatus() == 4) {
                throw new BusinessException("该订单已退票");
            } else if (order.getStatus() == 5) {
                throw new BusinessException("该订单已过期");
            }
            throw new BusinessException("订单状态异常，无法支付");
        }

        // 获取场次，检查是否已开场
        Schedule schedule = scheduleMapper.selectById(order.getScheduleId());
        if (schedule != null && schedule.getStartTime() != null
            && schedule.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("场次已开始放映，无法支付");
        }

        // 将关联的座位标记为已售出
        if (order.getSeatNumbers() == null || order.getSeatNumbers().isEmpty()) {
            throw new BusinessException("订单座位信息异常，无法支付");
        }
        String[] seatNumberArr = order.getSeatNumbers().split(",");
        for (String seatNumber : seatNumberArr) {
            Seat seat = seatMapper.selectByScheduleAndNumber(order.getScheduleId(), seatNumber.trim());
            if (seat != null) {
                seatMapper.markSold(seat.getId());
            }
        }

        // 处理余额支付
        if ("BALANCE".equals(order.getPaymentMethod())) {
            memberService.payWithBalance(userId, order.getTotalPrice());
            log.info("余额支付: userId={}, amount={}", userId, order.getTotalPrice());
        }

        // 更新订单状态
        order.setStatus(1);  // 待观影
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 记录操作日志
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(order.getId());
        logEntry.setOperationType("PAY");
        logEntry.setBeforeContent("待支付");
        logEntry.setAfterContent("已支付, 支付时间: " + order.getPayTime() + ", 支付方式: " + order.getPaymentMethod());
        logEntry.setOperatorId(userId);
        logEntry.setOperatorType("USER");
        logEntry.setRemark("用户完成支付");
        orderLogMapper.insert(logEntry);

        // 10. 累积积分并自动升级会员等级（B15会员体系）
        try {
            memberService.accumulatePoints(userId, order.getTotalPrice());
            log.info("积分累积完成: userId={}, 消费金额={}", userId, order.getTotalPrice());
        } catch (Exception e) {
            // 积分累积失败不影响支付成功的结果
            log.error("积分累积失败（支付已成功）: userId={}, amount={}, error={}",
                userId, order.getTotalPrice(), e.getMessage());
        }

        log.info("订单支付成功: orderNo={}, 金额={}", order.getOrderNo(), order.getTotalPrice());

        return fillOrderInfo(order);
    }

    /**
     * 改签
     * 核心流程：
     * 1. 验证原订单状态为待观影(1)且原场次未开始
     * 2. 验证新场次存在且有效、新座位可用
     * 3. 释放原座位（状态1->0）
     * 4. 锁定新座位（状态0->1）
     * 5. 将原订单状态改为已改签(3)
     * 6. 创建新订单，设置originalOrderId为原订单ID
     * 7. 记录操作日志
     *
     * @param request 改签请求
     * @param userId  用户ID
     * @return 改签后的新订单
     */
    @Override
    @Transactional
    public Order reschedule(RescheduleRequest request, Long userId) {
        // 1. 验证原订单
        Order oldOrder = orderMapper.selectById(request.getOrderId());
        if (oldOrder == null) {
            throw new BusinessException("原订单不存在");
        }
        if (!oldOrder.getUserId().equals(userId)) {
            throw new BusinessException("无权操作他人订单");
        }
        if (oldOrder.getStatus() != 1) {
            throw new BusinessException("只有待观影状态的订单才能改签");
        }

        // 验证原场次是否已开始
        Schedule oldSchedule = scheduleMapper.selectById(oldOrder.getScheduleId());
        if (oldSchedule != null && oldSchedule.getStartTime() != null
            && oldSchedule.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("原场次已开始放映，无法改签");
        }

        // 2. 验证新场次
        Schedule newSchedule = scheduleMapper.selectById(request.getNewScheduleId());
        if (newSchedule == null) {
            throw new BusinessException("新场次不存在");
        }
        if (newSchedule.getStatus() != null && newSchedule.getStatus() != 1) {
            throw new BusinessException("新场次已取消或已结束");
        }
        if (newSchedule.getStartTime() != null && newSchedule.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("新场次已开始放映");
        }

        // 不能改签到同一个场次
        if (request.getNewScheduleId().equals(oldOrder.getScheduleId())) {
            throw new BusinessException("不能改签到同一场次，请选择其他场次");
        }

        // 3. 验证新座位可用
        List<String> newSeatNumbers = request.getNewSeatNumbers();
        for (String seatNumber : newSeatNumbers) {
            Seat seat = seatMapper.selectByScheduleAndNumber(request.getNewScheduleId(), seatNumber);
            if (seat == null) {
                throw new BusinessException("新场次座位 " + seatNumber + " 不存在");
            }
            if (seat.getStatus() != 0) {
                throw new BusinessException("新场次座位 " + seatNumber + " 已被占用");
            }
        }

        // 4. 释放原订单的座位
        seatMapper.releaseSeatsByOrderId(oldOrder.getId());

        // 原子减少原场次已售数量
        if (oldSchedule != null) {
            scheduleMapper.decrementSoldCount(oldSchedule.getId(), oldOrder.getSeatCount());
        }

        // 5. 将原订单标记为已改签
        String oldOrderInfo = "订单号: " + oldOrder.getOrderNo() + ", 场次: " + oldOrder.getScheduleId()
            + ", 座位: " + oldOrder.getSeatNumbers();
        oldOrder.setStatus(3);
        oldOrder.setRescheduleTime(LocalDateTime.now());
        orderMapper.updateById(oldOrder);

        // 6. 计算新旧票价
        BigDecimal oldTotalPrice = oldOrder.getTotalPrice() != null ? oldOrder.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal newUnitPrice = newSchedule.getPrice() != null ? newSchedule.getPrice() : BigDecimal.ZERO;
        BigDecimal newTotalPrice = newUnitPrice.multiply(BigDecimal.valueOf(newSeatNumbers.size()));
        BigDecimal priceDiff = newTotalPrice.subtract(oldTotalPrice); // 正数=需补差价，负数=需退款

        // 7. 创建新订单
        String newOrderNo = generateOrderNo();
        Order newOrder = new Order();
        newOrder.setOrderNo(newOrderNo);
        newOrder.setUserId(userId);
        newOrder.setScheduleId(request.getNewScheduleId());
        newOrder.setMovieId(newSchedule.getMovieId());
        newOrder.setHallId(newSchedule.getHallId());
        newOrder.setSeatNumbers(String.join(",", newSeatNumbers));
        newOrder.setSeatCount(newSeatNumbers.size());
        newOrder.setTotalPrice(newTotalPrice);
        newOrder.setOriginalOrderId(oldOrder.getId());

        // 根据价差决定新订单状态
        String priceDiffDesc;
        if (priceDiff.compareTo(BigDecimal.ZERO) > 0) {
            // 新票价 > 旧票价：新订单为待支付状态，需补差价
            newOrder.setStatus(0);
            priceDiffDesc = "需补差价 $" + priceDiff;
        } else {
            // 新票价 <= 旧票价：新订单直接为待观影状态
            newOrder.setStatus(1);
            newOrder.setPayTime(LocalDateTime.now());
            priceDiffDesc = priceDiff.compareTo(BigDecimal.ZERO) < 0
                ? "需退款 $" + priceDiff.abs() + "（已退至原订单备注）" : "无差价";
        }
        orderMapper.insert(newOrder);

        // 锁定新座位
        for (String seatNumber : newSeatNumbers) {
            Seat seat = seatMapper.selectByScheduleAndNumber(request.getNewScheduleId(), seatNumber);
            if (seat != null) {
                seatMapper.lockSeat(seat.getId(), newOrder.getId());
                if (newOrder.getStatus() == 1) {
                    // 只有直接生效的订单才标记已售出；待支付的先锁定
                    seatMapper.markSold(seat.getId());
                }
            }
        }

        // 原子调整新场次已售数量（仅已支付状态计入）
        if (newOrder.getStatus() == 1) {
            scheduleMapper.incrementSoldCount(newSchedule.getId(), newSeatNumbers.size());
        }

        // 8. 记录原订单改签备注（含价差信息）
        oldOrder.setRescheduleTime(LocalDateTime.now());
        orderMapper.updateById(oldOrder);

        // 9. 记录操作日志
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(oldOrder.getId());
        logEntry.setOperationType("RESCHEDULE");
        logEntry.setBeforeContent(oldOrderInfo);
        logEntry.setAfterContent("改签至新订单: " + newOrderNo + ", 场次: " + request.getNewScheduleId()
            + ", 座位: " + String.join(",", newSeatNumbers)
            + " | 原价: $" + oldTotalPrice + ", 新价: $" + newTotalPrice + ", " + priceDiffDesc);
        logEntry.setOperatorId(userId);
        logEntry.setOperatorType("USER");
        logEntry.setRemark("用户改签 (" + priceDiffDesc + ")");
        orderLogMapper.insert(logEntry);

        log.info("改签成功: 原订单={}, 新订单={}, 原价={}, 新价={}, {}",
            oldOrder.getOrderNo(), newOrderNo, oldTotalPrice, newTotalPrice, priceDiffDesc);

        return fillOrderInfo(newOrder);
    }

    /**
     * 退票
     * 核心流程：
     * 1. 验证订单状态为待观影(1)且场次未开始
     * 2. 释放所有座位
     * 3. 将订单状态改为已退票(4)
     * 4. 记录操作日志
     *
     * @param orderId 订单ID
     * @param userId  退票用户ID
     */
    @Override
    @Transactional
    public void refund(Long orderId, Long userId) {
        // 1. 验证订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作他人订单");
        }
        if (order.getStatus() != 1) {
            if (order.getStatus() == 3) {
                throw new BusinessException("该订单已改签，无法退票");
            } else if (order.getStatus() == 4) {
                throw new BusinessException("该订单已退票，请勿重复操作");
            }
            throw new BusinessException("订单状态异常，无法退票");
        }

        // 验证场次是否已开始
        Schedule schedule = scheduleMapper.selectById(order.getScheduleId());
        if (schedule != null && schedule.getStartTime() != null
            && schedule.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("场次已开始放映，无法退票");
        }

        // 计算退票手续费
        BigDecimal refundFee = calculateRefundFee(schedule, order.getTotalPrice());
        BigDecimal refundAmount = order.getTotalPrice() != null
            ? order.getTotalPrice().subtract(refundFee) : BigDecimal.ZERO;

        // 2. 释放座位
        seatMapper.releaseSeatsByOrderId(order.getId());

        // 原子减少场次已售数量
        if (schedule != null) {
            scheduleMapper.decrementSoldCount(schedule.getId(), order.getSeatCount());
        }

        // 3. 更新订单状态
        String feeInfo = refundFee.compareTo(BigDecimal.ZERO) > 0
            ? "，手续费: $" + refundFee + "，实际退款: $" + refundAmount
            : "";
        String orderInfo = "订单号: " + order.getOrderNo() + ", 场次: " + order.getScheduleId()
            + ", 座位: " + order.getSeatNumbers() + ", 金额: " + order.getTotalPrice() + feeInfo;
        order.setStatus(4);  // 已退票
        order.setRescheduleTime(LocalDateTime.now());  // 记录退票操作时间
        orderMapper.updateById(order);

        // 4. 记录操作日志
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(order.getId());
        logEntry.setOperationType("REFUND");
        logEntry.setBeforeContent(orderInfo);
        logEntry.setAfterContent("已退票, 退款金额: " + order.getTotalPrice());
        logEntry.setOperatorId(userId);
        logEntry.setOperatorType("USER");
        logEntry.setRemark("用户退票");
        orderLogMapper.insert(logEntry);

        log.info("退票成功: orderNo={}, 退款金额={}, 用户={}", order.getOrderNo(), order.getTotalPrice(), userId);
    }

    /**
     * 取消未支付订单（手动取消）
     * 核心流程：
     * 1. 验证订单状态为待支付(0)且场次未开始
     * 2. 使用乐观锁将订单状态改为已取消(5)
     * 3. 释放所有座位
     * 4. 记录操作日志
     *
     * 与退款不同：取消仅适用于未支付订单，无需计算手续费
     *
     * @param orderId 订单ID
     * @param userId  取消用户ID
     */
    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        // 1. 验证订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作他人订单");
        }
        if (order.getStatus() != 0) {
            if (order.getStatus() == 1) {
                throw new BusinessException("该订单已支付，请操作退票");
            } else if (order.getStatus() == 4) {
                throw new BusinessException("该订单已退票");
            } else if (order.getStatus() == 5) {
                throw new BusinessException("该订单已取消");
            }
            throw new BusinessException("订单状态异常，无法取消");
        }

        // 验证场次是否已开始
        Schedule schedule = scheduleMapper.selectById(order.getScheduleId());
        if (schedule != null && schedule.getStartTime() != null
            && schedule.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("场次已开始放映，无法取消");
        }

        // 2. 乐观锁取消订单：仅当status=0时才更新为status=5
        int updated = orderMapper.cancelIfUnpaid(order.getId());
        if (updated == 0) {
            throw new BusinessException("订单状态已变更，取消失败");
        }

        // 3. 释放座位（乐观锁：只释放status=1的已锁定座位）
        seatMapper.releaseSeatsByOrderIdOptimistic(order.getId());

        // 原子减少场次已售数量
        if (schedule != null) {
            scheduleMapper.decrementSoldCount(schedule.getId(), order.getSeatCount());
        }

        // 4. 记录操作日志
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(order.getId());
        logEntry.setOperationType("CANCEL");
        logEntry.setBeforeContent("待支付, 订单号: " + order.getOrderNo()
            + ", 座位: " + order.getSeatNumbers() + ", 金额: " + order.getTotalPrice());
        logEntry.setAfterContent("用户主动取消订单，座位已释放");
        logEntry.setOperatorId(userId);
        logEntry.setOperatorType("USER");
        logEntry.setRemark("用户手动取消未支付订单");
        orderLogMapper.insert(logEntry);

        log.info("订单取消成功: orderNo={}, 用户={}, 座位已释放", order.getOrderNo(), userId);
    }

    /**
     * 查询用户的所有订单
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 分页订单列表
     */
    @Override
    public Page<Order> listByUser(Long userId, int page, int size) {
        // 使用数据库级别分页，避免OOM
        Page<Order> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> result = orderMapper.selectPage(pageParam, wrapper);
        // 批量补充关联信息，避免N+1查询
        batchFillOrderInfo(result.getRecords());
        return result;
    }

    /**
     * 查询所有订单（管理端）
     *
     * @param page   页码
     * @param size   每页大小
     * @param status 状态筛选
     * @return 分页订单列表
     */
    @Override
    public Page<Order> listAll(int page, int size, Integer status) {
        Page<Order> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> result = orderMapper.selectPage(pageParam, wrapper);
        // 批量填充关联信息，避免N+1查询
        batchFillOrderInfo(result.getRecords());
        return result;
    }

    /**
     * 查询订单详情
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    @Override
    public Order detail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return fillOrderInfo(order);
    }

    /**
     * 取消过期未支付订单
     * 定时任务，每2分钟执行一次
     * 查询创建超过配置超时分钟且状态仍为待支付(0)的订单
     * 使用乐观锁UPDATE防止支付竞态条件：
     * - 先尝试原子UPDATE status=5 WHERE id=? AND status=0
     * - 只有成功UPDATE的订单才释放座位，避免用户在临界点支付后被错误取消
     */
    @Override
    @Scheduled(fixedDelay = 120000) // 每2分钟执行一次
    public void cancelExpired() {
        // 从系统配置读取超时分钟数，默认15分钟
        int timeoutMinutes = getOrderTimeoutMinutes();
        List<Order> expiredOrders = orderMapper.selectExpiredOrders(timeoutMinutes);
        if (expiredOrders == null || expiredOrders.isEmpty()) {
            return;
        }

        int count = 0;
        for (Order order : expiredOrders) {
            try {
                // 乐观锁UPDATE：只有status仍为0的订单才能被标记为过期
                // 如果用户在临界点刚支付完成，status已变为1，此UPDATE不会影响任何行
                int updated = orderMapper.cancelIfUnpaid(order.getId());
                if (updated == 0) {
                    // 订单已被支付，跳过
                    log.debug("订单{}状态已变更，跳过取消", order.getOrderNo());
                    continue;
                }

                // 释放座位（使用乐观锁：只释放status=1的已锁定座位）
                seatMapper.releaseSeatsByOrderIdOptimistic(order.getId());

                // 原子减少场次已售数量（仅当订单占用了sold_count时）
                Schedule schedule = scheduleMapper.selectById(order.getScheduleId());
                if (schedule != null) {
                    scheduleMapper.decrementSoldCount(schedule.getId(), order.getSeatCount());
                }

                // 记录日志
                OrderLog logEntry = new OrderLog();
                logEntry.setOrderId(order.getId());
                logEntry.setOperationType("EXPIRE");
                logEntry.setBeforeContent("待支付, 订单号: " + order.getOrderNo());
                logEntry.setAfterContent("超时自动取消, 座位: " + order.getSeatNumbers());
                logEntry.setOperatorId(0L);
                logEntry.setOperatorType("SYSTEM");
                logEntry.setRemark("系统自动取消超时未支付订单");
                orderLogMapper.insert(logEntry);

                count++;
            } catch (Exception e) {
                log.error("取消过期订单失败: orderId={}, error={}", order.getId(), e.getMessage());
            }
        }
        if (count > 0) {
            log.info("定时任务: 已自动取消 {} 个过期未支付订单", count);
        }
    }

    /**
     * 计算退票手续费
     * 策略：
     * - 开场前24小时以上：免手续费
     * - 开场前2-24小时：收取20%手续费
     * - 开场前2小时内：收取50%手续费
     *
     * @param schedule   场次
     * @param totalPrice 订单总价
     * @return 手续费金额
     */
    private BigDecimal calculateRefundFee(Schedule schedule, BigDecimal totalPrice) {
        if (schedule == null || schedule.getStartTime() == null || totalPrice == null) {
            return BigDecimal.ZERO;
        }
        long hoursUntilStart = java.time.Duration.between(LocalDateTime.now(), schedule.getStartTime()).toHours();
        if (hoursUntilStart >= 24) {
            return BigDecimal.ZERO;
        } else if (hoursUntilStart >= 2) {
            return totalPrice.multiply(new BigDecimal("0.20")).setScale(2, java.math.RoundingMode.HALF_UP);
        } else {
            return totalPrice.multiply(new BigDecimal("0.50")).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    /**
     * 从系统配置表读取订单超时分钟数
     */
    private int getOrderTimeoutMinutes() {
        try {
            SystemConfig config = systemConfigMapper.selectByKey("order_timeout");
            if (config != null && config.getConfigValue() != null) {
                return Integer.parseInt(config.getConfigValue().trim());
            }
        } catch (Exception e) {
            log.warn("读取order_timeout配置失败，使用默认15分钟", e);
        }
        return 15;
    }

    /**
     * 协助创建订单（管理端替用户下单）
     *
     * @param request    订单请求
     * @param userId     目标用户ID
     * @param operatorId 操作员工ID
     * @return 创建的订单
     */
    @Override
    @Transactional
    public Order assistCreate(OrderRequest request, Long userId, Long operatorId) {
        Order order = createOrder(request, userId);

        // 记录操作日志（覆盖createOrder中的日志，标记为管理员协助）
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(order.getId());
        logEntry.setOperationType("CREATE");
        logEntry.setBeforeContent("");
        logEntry.setAfterContent("管理员协助创建订单, 座位: " + order.getSeatNumbers());
        logEntry.setOperatorId(operatorId);
        logEntry.setOperatorType("EMPLOYEE");
        logEntry.setRemark("管理员(" + operatorId + ")协助用户(" + userId + ")下单");
        orderLogMapper.insert(logEntry);

        log.info("管理员协助下单成功: orderNo={}, 用户={}, 操作员={}", order.getOrderNo(), userId, operatorId);
        return order;
    }

    /**
     * 协助支付订单（管理端替用户支付）
     * 与普通支付流程相同，但跳过用户所有权校验，使用操作员工ID记录日志
     *
     * @param orderId    订单ID
     * @param operatorId 操作员工ID
     * @return 支付后的订单
     */
    @Override
    @Transactional
    public Order assistPay(Long orderId, Long operatorId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        // 状态校验：只有待支付状态的订单才能支付
        if (order.getStatus() != 0) {
            if (order.getStatus() == 1) {
                throw new BusinessException("该订单已支付，请勿重复支付");
            } else if (order.getStatus() == 3) {
                throw new BusinessException("该订单已改签");
            } else if (order.getStatus() == 4) {
                throw new BusinessException("该订单已退票");
            } else if (order.getStatus() == 5) {
                throw new BusinessException("该订单已过期");
            }
            throw new BusinessException("订单状态异常，无法支付");
        }

        // 获取场次，检查是否已开场
        Schedule schedule = scheduleMapper.selectById(order.getScheduleId());
        if (schedule != null && schedule.getStartTime() != null
            && schedule.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("场次已开始放映，无法支付");
        }

        // 将关联的座位标记为已售出
        if (order.getSeatNumbers() == null || order.getSeatNumbers().isEmpty()) {
            throw new BusinessException("订单座位信息异常，无法支付");
        }
        String[] seatNumberArr = order.getSeatNumbers().split(",");
        for (String seatNumber : seatNumberArr) {
            Seat seat = seatMapper.selectByScheduleAndNumber(order.getScheduleId(), seatNumber.trim());
            if (seat != null) {
                seatMapper.markSold(seat.getId());
            }
        }

        // 更新订单状态
        order.setStatus(1);  // 待观影
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 记录操作日志
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(order.getId());
        logEntry.setOperationType("PAY");
        logEntry.setBeforeContent("待支付");
        logEntry.setAfterContent("管理员协助支付, 支付时间: " + order.getPayTime());
        logEntry.setOperatorId(operatorId);
        logEntry.setOperatorType("EMPLOYEE");
        logEntry.setRemark("管理员(" + operatorId + ")协助支付订单");
        orderLogMapper.insert(logEntry);

        // 累积积分并自动升级会员等级（B15会员体系）
        try {
            memberService.accumulatePoints(order.getUserId(), order.getTotalPrice());
        } catch (Exception e) {
            log.error("积分累积失败（协助支付已成功）: userId={}, amount={}, error={}",
                order.getUserId(), order.getTotalPrice(), e.getMessage());
        }

        log.info("管理员协助支付成功: orderNo={}, 操作员={}", order.getOrderNo(), operatorId);

        return fillOrderInfo(order);
    }

    /**
     * 协助退票（管理端替用户退票）
     * 与普通退票流程相同，但跳过用户所有权校验，使用操作员工ID记录日志
     *
     * @param orderId    订单ID
     * @param operatorId 操作员工ID
     * @return 退票后的订单
     */
    @Override
    @Transactional
    public Order assistRefund(Long orderId, Long operatorId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        // 状态校验：只有待观影状态的订单才能退票
        if (order.getStatus() != 1) {
            if (order.getStatus() == 3) {
                throw new BusinessException("该订单已改签，无法退票");
            } else if (order.getStatus() == 4) {
                throw new BusinessException("该订单已退票，请勿重复操作");
            } else if (order.getStatus() == 0) {
                throw new BusinessException("该订单尚未支付，无需退票");
            }
            throw new BusinessException("订单状态异常，无法退票");
        }

        // 验证场次是否已开始
        Schedule schedule = scheduleMapper.selectById(order.getScheduleId());
        if (schedule != null && schedule.getStartTime() != null
            && schedule.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("场次已开始放映，无法退票");
        }

        // 计算退票手续费
        BigDecimal refundFee = calculateRefundFee(schedule, order.getTotalPrice());
        BigDecimal refundAmount = order.getTotalPrice() != null
            ? order.getTotalPrice().subtract(refundFee) : BigDecimal.ZERO;

        // 释放座位
        seatMapper.releaseSeatsByOrderId(order.getId());

        // 原子减少场次已售数量
        if (schedule != null) {
            int sc = order.getSeatCount() != null ? order.getSeatCount() : order.getSeatNumbers() != null
                ? order.getSeatNumbers().split(",").length : 0;
            scheduleMapper.decrementSoldCount(schedule.getId(), sc);
        }

        // 更新订单状态
        String feeInfo = refundFee.compareTo(BigDecimal.ZERO) > 0
            ? "，手续费: ¥" + refundFee + "，实际退款: ¥" + refundAmount
            : "";
        String orderInfo = "订单号: " + order.getOrderNo() + ", 场次: " + order.getScheduleId()
            + ", 座位: " + order.getSeatNumbers() + ", 金额: " + order.getTotalPrice() + feeInfo;
        order.setStatus(4);  // 已退票
        order.setRescheduleTime(LocalDateTime.now());  // 记录退票操作时间
        orderMapper.updateById(order);

        // 记录操作日志
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(order.getId());
        logEntry.setOperationType("REFUND");
        logEntry.setBeforeContent(orderInfo);
        logEntry.setAfterContent("管理员协助退票, 退款金额: " + refundAmount);
        logEntry.setOperatorId(operatorId);
        logEntry.setOperatorType("EMPLOYEE");
        logEntry.setRemark("管理员(" + operatorId + ")协助退票" + feeInfo);
        orderLogMapper.insert(logEntry);

        log.info("管理员协助退票成功: orderNo={}, 操作员={}, 退款金额={}",
            order.getOrderNo(), operatorId, refundAmount);

        return fillOrderInfo(order);
    }

    /**
     * 生成唯一订单号
     * 格式: yyyyMMdd + 8位随机字母数字字符
     *
     * @return 订单号
     */
    private String generateOrderNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        StringBuilder randomPart = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            randomPart.append(RANDOM_CHARS.charAt(RANDOM.nextInt(RANDOM_CHARS.length())));
        }
        String orderNo = datePart + randomPart.toString();

        // 检查订单号是否已存在（极小概率，但确保唯一性）
        Order existing = orderMapper.selectByOrderNo(orderNo);
        if (existing != null) {
            return generateOrderNo(); // 递归重试
        }
        return orderNo;
    }

    /**
     * 补充订单的关联信息
     * 包括影片名、影厅名、用户名、场次时间等
     *
     * @param order 订单实体
     * @return 补充信息后的订单
     */
    private Order fillOrderInfo(Order order) {
        if (order == null) {
            return null;
        }
        // 影片信息
        if (order.getMovieId() != null) {
            Movie movie = movieMapper.selectById(order.getMovieId());
            if (movie != null) {
                order.setMovieName(movie.getMovieName());
                order.setMoviePoster(movie.getPosterUrl());
            }
        }
        // 影厅信息
        if (order.getHallId() != null) {
            Hall hall = hallMapper.selectById(order.getHallId());
            if (hall != null) {
                order.setHallName(hall.getHallName());
            }
        }
        // 场次时间信息
        if (order.getScheduleId() != null) {
            Schedule schedule = scheduleMapper.selectById(order.getScheduleId());
            if (schedule != null) {
                order.setStartTime(schedule.getStartTime());
                order.setEndTime(schedule.getEndTime());
            }
        }
        // 用户信息
        if (order.getUserId() != null) {
            User user = userMapper.selectById(order.getUserId());
            if (user != null) {
                order.setUsername(user.getUsername());
            }
        }
        return order;
    }

    /**
     * 批量填充订单关联信息（避免N+1查询）
     * 一次性查询所有关联的影片、影厅、场次，然后在内存中映射
     *
     * @param orders 订单列表
     */
    private void batchFillOrderInfo(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }

        // 收集所有关联ID
        List<Long> movieIds = orders.stream()
            .map(Order::getMovieId).filter(id -> id != null).distinct().collect(Collectors.toList());
        List<Long> hallIds = orders.stream()
            .map(Order::getHallId).filter(id -> id != null).distinct().collect(Collectors.toList());
        List<Long> scheduleIds = orders.stream()
            .map(Order::getScheduleId).filter(id -> id != null).distinct().collect(Collectors.toList());
        List<Long> userIds = orders.stream()
            .map(Order::getUserId).filter(id -> id != null).distinct().collect(Collectors.toList());

        // 批量查询
        Map<Long, Movie> movieMap = movieIds.isEmpty() ? Collections.emptyMap()
            : movieMapper.selectBatchIds(movieIds).stream()
                .collect(Collectors.toMap(Movie::getId, m -> m));
        Map<Long, Hall> hallMap = hallIds.isEmpty() ? Collections.emptyMap()
            : hallMapper.selectBatchIds(hallIds).stream()
                .collect(Collectors.toMap(Hall::getId, h -> h));
        Map<Long, Schedule> scheduleMap = scheduleIds.isEmpty() ? Collections.emptyMap()
            : scheduleMapper.selectBatchIds(scheduleIds).stream()
                .collect(Collectors.toMap(Schedule::getId, s -> s));
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
            : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 填充每个订单的关联信息
        for (Order order : orders) {
            if (order.getMovieId() != null) {
                Movie movie = movieMap.get(order.getMovieId());
                if (movie != null) {
                    order.setMovieName(movie.getMovieName());
                    order.setMoviePoster(movie.getPosterUrl());
                }
            }
            if (order.getHallId() != null) {
                Hall hall = hallMap.get(order.getHallId());
                if (hall != null) order.setHallName(hall.getHallName());
            }
            if (order.getScheduleId() != null) {
                Schedule schedule = scheduleMap.get(order.getScheduleId());
                if (schedule != null) {
                    order.setStartTime(schedule.getStartTime());
                    order.setEndTime(schedule.getEndTime());
                }
            }
            if (order.getUserId() != null) {
                User user = userMap.get(order.getUserId());
                if (user != null) order.setUsername(user.getUsername());
            }
        }
    }
}
