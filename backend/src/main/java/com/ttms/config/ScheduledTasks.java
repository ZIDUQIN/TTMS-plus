package com.ttms.config;

import com.ttms.entity.Movie;
import com.ttms.entity.Order;
import com.ttms.entity.Schedule;
import com.ttms.mapper.MovieMapper;
import com.ttms.mapper.OrderLogMapper;
import com.ttms.mapper.OrderMapper;
import com.ttms.mapper.ScheduleMapper;
import com.ttms.mapper.SeatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统定时任务组件
 * 包含场次自动结束、过期座位清理、操作日志归档等周期性维护任务
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTasks {

    private final ScheduleMapper scheduleMapper;
    private final SeatMapper seatMapper;
    private final OrderLogMapper orderLogMapper;
    private final MovieMapper movieMapper;
    private final OrderMapper orderMapper;

    /**
     * 场次自动结束任务
     * 每分钟检查一次：将 end_time < NOW() 且 status=1 的场次标记为已结束(status=2)
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void autoEndSchedules() {
        try {
            List<Schedule> activeSchedules = scheduleMapper.selectActiveExpired();
            if (activeSchedules == null || activeSchedules.isEmpty()) {
                return;
            }
            int count = 0;
            for (Schedule s : activeSchedules) {
                scheduleMapper.markEnded(s.getId());
                count++;
            }
            if (count > 0) {
                log.info("定时任务: 自动结束 {} 个已放映完成的场次", count);
            }
        } catch (Exception e) {
            log.error("自动结束场次任务异常", e);
        }
    }

    /**
     * 过期锁定座位清理任务
     * 每5分钟检查一次：释放lock_time超过30分钟且status=1的座位
     * 兜底机制：防止系统重启后创建的订单锁定座位未被释放
     */
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void releaseStaleLockedSeats() {
        try {
            int released = seatMapper.releaseStaleLockedSeats(30);
            if (released > 0) {
                log.warn("定时任务: 释放 {} 个超时锁定的座位（超过30分钟未支付）", released);
            }
        } catch (Exception e) {
            log.error("释放过期锁定座位任务异常", e);
        }
    }

    /**
     * 操作日志自动清理任务
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanOldOrderLogs() {
        try {
            int deleted = orderLogMapper.deleteOlderThan(90);
            if (deleted > 0) log.info("定时任务: 已清理 {} 条90天前的操作日志", deleted);
        } catch (Exception e) {
            log.error("清理过期操作日志任务异常", e);
        }
    }

    /**
     * B27: 影片自动上下架
     * 每小时检查：release_date已到的→自动上架，已过期30天的→标记下架提醒
     */
    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void autoUpdateMovieStatus() {
        try {
            LocalDate today = LocalDate.now();
            // 即将上映→上架：release_date <= 今天
            List<Movie> upcoming = movieMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Movie>()
                    .eq(Movie::getStatus, 2).le(Movie::getReleaseDate, today));
            for (Movie m : upcoming) {
                m.setStatus(1);
                movieMapper.updateById(m);
                log.info("影片自动上架: id={}, name={}", m.getId(), m.getMovieName());
            }
            if (!upcoming.isEmpty()) log.info("定时任务: {} 部影片自动上架", upcoming.size());
        } catch (Exception e) {
            log.error("影片自动上下架任务异常", e);
        }
    }

    /**
     * B21: 支付超时提醒
     * 每2分钟检查：status=0且创建超过12分钟的订单，记录提醒
     */
    @Scheduled(fixedDelay = 120000)
    @Transactional
    public void remindPendingPayment() {
        try {
            List<Order> remindOrders = orderMapper.selectExpiredOrders(12);
            if (remindOrders == null || remindOrders.isEmpty()) return;
            int reminded = 0;
            for (Order o : remindOrders) {
                log.info("支付提醒: orderNo={}, userId={}, remaining=3min", o.getOrderNo(), o.getUserId());
                reminded++;
                if (reminded >= 100) break; // 防止过量
            }
            if (reminded > 0) log.info("定时任务: 已发送 {} 条支付超时提醒", reminded);
        } catch (Exception e) {
            log.error("支付超时提醒任务异常", e);
        }
    }
}
