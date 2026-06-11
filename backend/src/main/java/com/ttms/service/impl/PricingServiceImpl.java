package com.ttms.service.impl;

import com.ttms.entity.Schedule;
import com.ttms.entity.Seat;
import com.ttms.entity.User;
import com.ttms.mapper.SeatMapper;
import com.ttms.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.List;

/**
 * 统一票价计算引擎
 * 整合座位分区定价(B9)、时段差异化定价(B10)、人群差异化定价(B11)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingServiceImpl {

    private final SeatMapper seatMapper;

    /** 时段定价规则 */
    private static final BigDecimal MORNING_DISCOUNT = new BigDecimal("0.50");   // 10:00前 5折
    private static final BigDecimal EVENING_PREMIUM = new BigDecimal("1.20");    // 18:00-21:00 溢价20%
    private static final BigDecimal LATE_DISCOUNT = new BigDecimal("0.80");      // 22:00后 8折

    /** 人群折扣 */
    private static final BigDecimal STUDENT_DISCOUNT = new BigDecimal("0.50");
    private static final BigDecimal CHILD_DISCOUNT = new BigDecimal("0.50");
    private static final BigDecimal SENIOR_DISCOUNT = new BigDecimal("0.50");
    private static final BigDecimal DISABLED_DISCOUNT = new BigDecimal("0.50");
    private static final BigDecimal MILITARY_DISCOUNT = new BigDecimal("0.80");

    /**
     * 计算单个座位的最终价格
     * @param basePrice 场次基础票价
     * @param seat 座位（含分区价格调整）
     * @param schedule 场次（判断时段）
     * @return 单个座位最终价格
     */
    public BigDecimal calculateSeatPrice(BigDecimal basePrice, Seat seat, Schedule schedule) {
        BigDecimal price = basePrice != null ? basePrice : BigDecimal.ZERO;

        // B9: 座位分区定价调整
        if (seat.getPriceAdjustment() != null) {
            price = price.add(seat.getPriceAdjustment());
        }

        // B10: 时段定价
        if (schedule != null && schedule.getStartTime() != null) {
            price = applyTimePricing(price, schedule.getStartTime().toLocalTime());
        }

        return price.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 应用人群折扣
     * @param seatPrice 座位基础价格
     * @param ticketType 票种: STUDENT/CHILD/SENIOR/DISABLED/MILITARY/null(普通)
     * @return 折扣后价格
     */
    public BigDecimal applyTicketTypeDiscount(BigDecimal seatPrice, String ticketType) {
        if (ticketType == null || seatPrice == null) return seatPrice;
        BigDecimal rate = switch (ticketType.toUpperCase()) {
            case "STUDENT" -> STUDENT_DISCOUNT;
            case "CHILD" -> CHILD_DISCOUNT;
            case "SENIOR" -> SENIOR_DISCOUNT;
            case "DISABLED" -> DISABLED_DISCOUNT;
            case "MILITARY" -> MILITARY_DISCOUNT;
            default -> BigDecimal.ONE;
        };
        return seatPrice.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算订单总价（含座位分区+时段+人群折扣）
     * @param schedule 场次
     * @param seatNumbers 座位编号列表
     * @param ticketTypes 每个座位对应的票种（与seatNumbers一一对应）
     * @return 订单总价
     */
    public BigDecimal calculateOrderTotal(Schedule schedule, List<String> seatNumbers, List<String> ticketTypes) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < seatNumbers.size(); i++) {
            Seat seat = seatMapper.selectByScheduleAndNumber(schedule.getId(), seatNumbers.get(i));
            if (seat == null) continue;
            BigDecimal seatPrice = calculateSeatPrice(schedule.getPrice(), seat, schedule);
            String ticketType = (ticketTypes != null && i < ticketTypes.size()) ? ticketTypes.get(i) : null;
            seatPrice = applyTicketTypeDiscount(seatPrice, ticketType);
            total = total.add(seatPrice);
        }
        return total;
    }

    /**
     * 应用时段定价倍数
     */
    private BigDecimal applyTimePricing(BigDecimal price, LocalTime time) {
        if (time.isBefore(LocalTime.of(10, 0))) {
            return price.multiply(MORNING_DISCOUNT);
        } else if (time.isAfter(LocalTime.of(18, 0)) && time.isBefore(LocalTime.of(21, 0))) {
            return price.multiply(EVENING_PREMIUM);
        } else if (time.isAfter(LocalTime.of(22, 0))) {
            return price.multiply(LATE_DISCOUNT);
        }
        return price;
    }
}
