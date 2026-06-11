package com.ttms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ttms.entity.Order;
import com.ttms.entity.Schedule;
import com.ttms.mapper.MovieMapper;
import com.ttms.mapper.OrderMapper;
import com.ttms.mapper.ScheduleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * B25: 数据看板服务
 * 提供管理端仪表盘所需的实时聚合数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl {

    private final OrderMapper orderMapper;
    private final ScheduleMapper scheduleMapper;
    private final MovieMapper movieMapper;

    public Map<String, Object> getDashboardData() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        // 今日营收
        Map<String, Object> todayRevenue = orderMapper.aggregateRevenue(todayStart, todayEnd);
        // 今日场次
        LambdaQueryWrapper<Schedule> scheduleWrapper = new LambdaQueryWrapper<>();
        scheduleWrapper.eq(Schedule::getDeleted, 0)
            .ge(Schedule::getStartTime, todayStart)
            .lt(Schedule::getStartTime, todayEnd);
        long todaySchedules = scheduleMapper.selectCount(scheduleWrapper);

        // 活跃影片数
        LambdaQueryWrapper<com.ttms.entity.Movie> movieWrapper = new LambdaQueryWrapper<>();
        movieWrapper.eq(com.ttms.entity.Movie::getStatus, 1);
        long activeMovies = movieMapper.selectCount(movieWrapper);

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("todayRevenue", todayRevenue.get("totalRevenue"));
        dashboard.put("todayOrders", todayRevenue.get("orderCount"));
        dashboard.put("todayTickets", todayRevenue.get("ticketCount"));
        dashboard.put("todaySchedules", todaySchedules);
        dashboard.put("activeMovies", activeMovies);
        dashboard.put("date", LocalDate.now().toString());
        return dashboard;
    }
}
