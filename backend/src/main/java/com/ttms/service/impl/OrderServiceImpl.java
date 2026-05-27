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
import java.util.List;
import java.util.Random;

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

        // 7. 计算总价
        BigDecimal unitPrice = schedule.getPrice() != null ? schedule.getPrice() : BigDecimal.ZERO;
        totalPrice = unitPrice.multiply(BigDecimal.valueOf(seatNumbers.size()));
        order.setTotalPrice(totalPrice);
        orderMapper.updateById(order);

        // 8. 增加场次已售数量（预占，实际支付后不需要再更新）
        schedule.setSoldCount((schedule.getSoldCount() != null ? schedule.getSoldCount() : 0) + seatNumbers.size());
        scheduleMapper.updateById(schedule);

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
        // 权限校验：只能支付自己的订单（管理员协助支付可在此扩展）
        // 此处不限制，因为管理员也有可能需要支付
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
        logEntry.setAfterContent("已支付, 支付时间: " + order.getPayTime());
        logEntry.setOperatorId(userId);
        logEntry.setOperatorType("USER");
        logEntry.setRemark("用户完成支付");
        orderLogMapper.insert(logEntry);

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

        // 更新原场次已售数量
        if (oldSchedule != null) {
            oldSchedule.setSoldCount(Math.max(0,
                (oldSchedule.getSoldCount() != null ? oldSchedule.getSoldCount() : 0) - oldOrder.getSeatCount()));
            scheduleMapper.updateById(oldSchedule);
        }

        // 5. 将原订单标记为已改签
        String oldOrderInfo = "订单号: " + oldOrder.getOrderNo() + ", 场次: " + oldOrder.getScheduleId()
            + ", 座位: " + oldOrder.getSeatNumbers();
        oldOrder.setStatus(3);
        oldOrder.setRescheduleTime(LocalDateTime.now());
        orderMapper.updateById(oldOrder);

        // 6. 创建新订单
        String newOrderNo = generateOrderNo();
        Order newOrder = new Order();
        newOrder.setOrderNo(newOrderNo);
        newOrder.setUserId(userId);
        newOrder.setScheduleId(request.getNewScheduleId());
        newOrder.setMovieId(newSchedule.getMovieId());
        newOrder.setHallId(newSchedule.getHallId());
        newOrder.setSeatNumbers(String.join(",", newSeatNumbers));
        newOrder.setSeatCount(newSeatNumbers.size());
        newOrder.setStatus(1);  // 改签订单默认已支付（因为是从已支付订单改签而来）
        newOrder.setPayTime(LocalDateTime.now());
        newOrder.setOriginalOrderId(oldOrder.getId());
        orderMapper.insert(newOrder);

        // 锁定新座位
        for (String seatNumber : newSeatNumbers) {
            Seat seat = seatMapper.selectByScheduleAndNumber(request.getNewScheduleId(), seatNumber);
            if (seat != null) {
                seatMapper.lockSeat(seat.getId(), newOrder.getId());
                seatMapper.markSold(seat.getId()); // 直接标记已售出
            }
        }

        // 计算新订单金额
        BigDecimal newUnitPrice = newSchedule.getPrice() != null ? newSchedule.getPrice() : BigDecimal.ZERO;
        BigDecimal newTotalPrice = newUnitPrice.multiply(BigDecimal.valueOf(newSeatNumbers.size()));
        newOrder.setTotalPrice(newTotalPrice);
        orderMapper.updateById(newOrder);

        // 更新新场次已售数量
        newSchedule.setSoldCount((newSchedule.getSoldCount() != null ? newSchedule.getSoldCount() : 0)
            + newSeatNumbers.size());
        scheduleMapper.updateById(newSchedule);

        // 7. 记录操作日志（原订单）
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(oldOrder.getId());
        logEntry.setOperationType("RESCHEDULE");
        logEntry.setBeforeContent(oldOrderInfo);
        logEntry.setAfterContent("改签至新订单: " + newOrderNo + ", 场次: " + request.getNewScheduleId()
            + ", 座位: " + String.join(",", newSeatNumbers));
        logEntry.setOperatorId(userId);
        logEntry.setOperatorType("USER");
        logEntry.setRemark("用户改签");
        orderLogMapper.insert(logEntry);

        log.info("改签成功: 原订单={}, 新订单={}, 用户={}", oldOrder.getOrderNo(), newOrderNo, userId);

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

        // 2. 释放座位
        seatMapper.releaseSeatsByOrderId(order.getId());

        // 更新场次已售数量
        if (schedule != null) {
            schedule.setSoldCount(Math.max(0,
                (schedule.getSoldCount() != null ? schedule.getSoldCount() : 0) - order.getSeatCount()));
            scheduleMapper.updateById(schedule);
        }

        // 3. 更新订单状态
        String orderInfo = "订单号: " + order.getOrderNo() + ", 场次: " + order.getScheduleId()
            + ", 座位: " + order.getSeatNumbers() + ", 金额: " + order.getTotalPrice();
        order.setStatus(4);  // 已退票
        order.setRescheduleTime(LocalDateTime.now());
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
     * 查询用户的所有订单
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 分页订单列表
     */
    @Override
    public Page<Order> listByUser(Long userId, int page, int size) {
        // 查询用户订单并分页
        List<Order> allUserOrders = orderMapper.selectByUserId(userId);

        Page<Order> pageParam = new Page<>(page, size);
        pageParam.setTotal(allUserOrders.size());

        int start = (int) ((page - 1) * size);
        int end = Math.min(start + size, allUserOrders.size());

        if (start < allUserOrders.size()) {
            pageParam.setRecords(allUserOrders.subList(start, end));
        } else {
            pageParam.setRecords(List.of());
        }

        return pageParam;
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
        for (Order order : result.getRecords()) {
            fillOrderInfo(order);
        }
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
     * 查询创建超过15分钟且状态仍为待支付(0)的订单
     * 释放座位、将订单状态改为已过期(5)
     */
    @Override
    @Scheduled(fixedDelay = 120000) // 每2分钟执行一次
    @Transactional
    public void cancelExpired() {
        List<Order> expiredOrders = orderMapper.selectExpiredOrders(15);
        if (expiredOrders == null || expiredOrders.isEmpty()) {
            return;
        }

        int count = 0;
        for (Order order : expiredOrders) {
            try {
                // 释放座位
                seatMapper.releaseSeatsByOrderId(order.getId());

                // 更新场次已售数量
                Schedule schedule = scheduleMapper.selectById(order.getScheduleId());
                if (schedule != null) {
                    schedule.setSoldCount(Math.max(0,
                        (schedule.getSoldCount() != null ? schedule.getSoldCount() : 0) - order.getSeatCount()));
                    scheduleMapper.updateById(schedule);
                }

                // 更新订单状态为已过期
                order.setStatus(5);
                orderMapper.updateById(order);

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
        return order;
    }
}
