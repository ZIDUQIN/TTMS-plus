package com.ttms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ttms.dto.ApiResponse;
import com.ttms.entity.*;
import com.ttms.mapper.*;
import com.ttms.service.ScheduleService;
import com.ttms.service.impl.MemberServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * B1: 线下柜台售票POS控制器
 * 提供柜台快速选场次、选座、多支付方式收款、打印小票
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/pos")
@RequiredArgsConstructor
public class PosController {

    private final ScheduleMapper scheduleMapper;
    private final SeatMapper seatMapper;
    private final OrderMapper orderMapper;
    private final MovieMapper movieMapper;
    private final HallMapper hallMapper;
    private final MemberServiceImpl memberService;
    private final ScheduleService scheduleService;

    /** 快速查询今日可用场次 */
    @GetMapping("/schedules")
    public ApiResponse<List<Map<String, Object>>> todaySchedules() {
        List<Schedule> schedules = scheduleMapper.selectUpcoming();
        List<Map<String, Object>> result = new ArrayList<>();

        // 批量查询所有场次的可用座位数，避免 N+1
        java.util.Set<Long> scheduleIds = schedules.stream()
            .map(Schedule::getId).collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, Integer> availableMap = new java.util.LinkedHashMap<>();
        if (!scheduleIds.isEmpty()) {
            List<Seat> allSeats = seatMapper.selectList(
                new LambdaQueryWrapper<Seat>()
                    .in(Seat::getScheduleId, scheduleIds)
                    .eq(Seat::getStatus, 0)); // 只统计空闲座位
            for (Seat seat : allSeats) {
                availableMap.merge(seat.getScheduleId(), 1, Integer::sum);
            }
        }

        for (Schedule s : schedules) {
            if (s.getStartTime() != null
                && s.getStartTime().toLocalDate().equals(java.time.LocalDate.now())) {
                Movie m = movieMapper.selectById(s.getMovieId());
                Hall h = hallMapper.selectById(s.getHallId());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", s.getId());
                item.put("movieId", s.getMovieId());
                item.put("movieName", m != null ? m.getMovieName() : "");
                item.put("hallName", h != null ? h.getHallName() : "");
                item.put("startTime", s.getStartTime());
                item.put("endTime", s.getEndTime());
                item.put("price", s.getPrice());
                // 优先使用seat表精确计数的空闲座位；若座位尚未生成则用影厅容量推算
                int available = availableMap.getOrDefault(s.getId(), -1);
                if (available < 0) {
                    int total = (s.getHallRowCount() != null && s.getHallColCount() != null)
                        ? s.getHallRowCount() * s.getHallColCount() : 0;
                    int sold = s.getSoldCount() != null ? s.getSoldCount() : 0;
                    available = Math.max(0, total - sold);
                }
                item.put("availableSeats", available);
                result.add(item);
            }
        }
        return ApiResponse.success(result);
    }

    /** 获取场次座位（柜台视图），自动生成座位（如尚未生成） */
    @GetMapping("/seats/{scheduleId}")
    public ApiResponse<Map<String, Object>> getSeats(@PathVariable Long scheduleId) {
        // 通过 ScheduleService 获取座位，内部会自动生成不存在的座位
        Map<String, Object> seatData = scheduleService.getSeats(scheduleId);
        return ApiResponse.success(seatData);
    }

    /** 柜台快速下单（支持现金/微信/支付宝） */
    @PostMapping("/create-order")
    @Transactional
    public ApiResponse<Map<String, Object>> createOrder(@RequestBody Map<String, Object> params) {
        Long scheduleId = Long.valueOf(params.get("scheduleId").toString());
        @SuppressWarnings("unchecked")
        List<String> seatNumbers = (List<String>) params.get("seatNumbers");
        String paymentMethod = params.getOrDefault("paymentMethod", "CASH").toString();

        if (seatNumbers == null || seatNumbers.isEmpty()) {
            return ApiResponse.error("请至少选择一个座位");
        }

        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) return ApiResponse.error("场次不存在");
        if (schedule.getStatus() != null && schedule.getStatus() != 1) {
            return ApiResponse.error("该场次已取消或已结束，无法售票");
        }

        // 验证座位是否存在且可用（状态为0-空闲）
        for (String sn : seatNumbers) {
            Seat seat = seatMapper.selectByScheduleAndNumber(scheduleId, sn);
            if (seat == null) {
                return ApiResponse.error("座位 " + sn + " 不存在");
            }
            if (seat.getStatus() != 0) {
                return ApiResponse.error("座位 " + sn + " 已被占用，请重新选择");
            }
        }

        // 生成唯一订单号并创建订单
        Long cashierId = getCurrentUserId();
        // 若指定了客户ID则关联客户，否则以操作员记名
        Long customerId = params.containsKey("userId")
            ? Long.valueOf(params.get("userId").toString()) : cashierId;

        String orderNo = generatePosOrderNo();
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setScheduleId(scheduleId);
        order.setMovieId(schedule.getMovieId());
        order.setHallId(schedule.getHallId());
        order.setSeatNumbers(String.join(",", seatNumbers));
        order.setSeatCount(seatNumbers.size());

        // 计算票价：基础票价 × 座位数
        BigDecimal rawTotal = schedule.getPrice().multiply(BigDecimal.valueOf(seatNumbers.size()));
        // B15: 若关联了真实客户，应用会员折扣
        BigDecimal memberDiscount = BigDecimal.ONE;
        if (params.containsKey("userId")) {
            try {
                memberDiscount = memberService.getDiscountRate(customerId);
            } catch (Exception e) {
                log.warn("POS获取会员折扣失败: userId={}, error={}", customerId, e.getMessage());
            }
        }
        BigDecimal finalPrice = rawTotal.multiply(memberDiscount).setScale(2, java.math.RoundingMode.HALF_UP);
        order.setTotalPrice(finalPrice);
        order.setUserId(customerId);   // 关联客户或操作员
        order.setStatus(1); // POS直接支付
        order.setPaymentMethod(paymentMethod);
        order.setCashierId(cashierId);
        order.setPayTime(LocalDateTime.now());

        // 插入订单（含重试机制应对极低概率的订单号碰撞）
        Exception lastError = null;
        for (int retry = 0; retry < 3; retry++) {
            try {
                orderMapper.insert(order);
                lastError = null;
                break;
            } catch (Exception e) {
                lastError = e;
                log.warn("POS订单插入失败(retry={}): orderNo={}, error={}",
                    retry + 1, orderNo, e.getMessage());
                // 重新生成订单号并重试
                orderNo = generatePosOrderNo();
                order.setOrderNo(orderNo);
            }
        }
        if (lastError != null) {
            log.error("POS订单插入最终失败: scheduleId={}, seats={}, error={}",
                scheduleId, seatNumbers, lastError.getMessage(), lastError);
            return ApiResponse.error("出票失败: " + lastError.getMessage());
        }

        // 标记座位已售出（乐观锁：仅更新 status=0 的座位，防止并发重复售出）
        for (String sn : seatNumbers) {
            int updated = seatMapper.markSoldByScheduleAndNumber(scheduleId, sn);
            if (updated == 0) {
                throw new RuntimeException("座位 " + sn + " 已被其他操作售出，请刷新后重试");
            }
        }

        scheduleMapper.incrementSoldCount(scheduleId, seatNumbers.size());

        // 累积积分并自动升级会员等级（B15会员体系）
        if (params.containsKey("userId")) {
            try {
                memberService.accumulatePoints(customerId, order.getTotalPrice());
            } catch (Exception e) {
                log.error("POS售票积分累积失败: userId={}, amount={}, error={}",
                    customerId, order.getTotalPrice(), e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", orderNo);
        result.put("totalPrice", finalPrice);
        result.put("originalPrice", rawTotal);
        if (memberDiscount.compareTo(BigDecimal.ONE) < 0) {
            result.put("memberDiscount", memberDiscount);
            result.put("memberDiscountLabel", ((1 - memberDiscount.doubleValue()) * 100) + "% off");
        }
        result.put("paymentMethod", paymentMethod);
        result.put("message", "柜台售票成功");
        log.info("POS售票: orderNo={}, seats={}, method={}, cashier={}, total={}, discount={}",
            orderNo, seatNumbers, paymentMethod, cashierId, finalPrice, memberDiscount);
        return ApiResponse.success(result);
    }

    /** 生成POS订单号：POS + yyyyMMdd + 毫秒时间戳后5位 + 3位随机数，确保唯一 */
    private String generatePosOrderNo() {
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 使用当前毫秒时间戳的后5位 + 3位随机数，极大降低碰撞概率
        long ms = System.currentTimeMillis() % 100000;
        int rand = new java.util.Random().nextInt(1000);
        return String.format("POS%s%05d%03d", datePart, ms, rand);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
