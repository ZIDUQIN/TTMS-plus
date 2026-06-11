package com.ttms.controller;

import com.ttms.dto.ApiResponse;
import com.ttms.entity.Order;
import com.ttms.exception.BusinessException;
import com.ttms.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * B3: 取票/检票控制器
 * 生成取票码、验证检票、防重复入场
 */
@Slf4j
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final OrderMapper orderMapper;
    /** 已检票记录: orderId -> 检票时间 */
    private final Map<Long, LocalDateTime> checkedInTickets = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 获取订单的取票信息（含取票码QR内容）
     * 取票码格式: TTMS-{orderNo}-{userId后4位}
     */
    @GetMapping("/pickup/{orderId}")
    public ApiResponse<Map<String, Object>> getPickupInfo(@PathVariable Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (order.getStatus() != 1) throw new BusinessException("订单状态异常，无法取票");

        String pickupCode = "TTMS-" + order.getOrderNo() + "-"
            + (order.getUserId() != null ? String.format("%04d", order.getUserId() % 10000) : "0000");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("orderNo", order.getOrderNo());
        info.put("pickupCode", pickupCode);
        info.put("movieId", order.getMovieId());
        info.put("hallId", order.getHallId());
        info.put("seats", order.getSeatNumbers());
        info.put("seatCount", order.getSeatCount());
        info.put("status", "可取票");
        return ApiResponse.success(info);
    }

    /**
     * 检票入场
     * 扫码验证取票码 → 检查是否已入场 → 标记入场
     */
    @PostMapping("/check-in")
    public ApiResponse<Map<String, Object>> checkIn(@RequestBody Map<String, String> params) {
        String pickupCode = params.get("code");
        if (pickupCode == null || pickupCode.isEmpty()) {
            return ApiResponse.error("请提供取票码");
        }

        // 解析取票码: TTMS-{orderNo}-{suffix}
        String[] parts = pickupCode.split("-");
        if (parts.length < 3) return ApiResponse.error("取票码格式无效");

        String orderNo = parts[1];
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) return ApiResponse.error("取票码对应的订单不存在");

        // 检查是否已入场
        if (checkedInTickets.containsKey(order.getId())) {
            return ApiResponse.error("该票已检票入场，时间: " + checkedInTickets.get(order.getId()));
        }

        // 标记入场
        checkedInTickets.put(order.getId(), LocalDateTime.now());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("seats", order.getSeatNumbers());
        result.put("checkInTime", LocalDateTime.now().toString());
        result.put("message", "检票通过，欢迎入场");
        log.info("检票入场: orderNo={}, seats={}", order.getOrderNo(), order.getSeatNumbers());
        return ApiResponse.success(result);
    }

    /** 检票统计 */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(Map.of("totalCheckedIn", checkedInTickets.size()));
    }
}
