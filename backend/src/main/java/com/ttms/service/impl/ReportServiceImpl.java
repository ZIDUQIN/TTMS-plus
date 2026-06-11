package com.ttms.service.impl;

import com.ttms.entity.Report;
import com.ttms.mapper.OrderMapper;
import com.ttms.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * B23: 报表服务
 * 日报/周报/月报自动生成 + 查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl {

    private final ReportMapper reportMapper;
    private final OrderMapper orderMapper;

    /** 每日凌晨2点生成昨日日报 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyReport() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        generateReport("DAILY", yesterday);
    }

    /** 每周一凌晨3点生成上周周报 */
    @Scheduled(cron = "0 0 3 * * 1")
    public void generateWeeklyReport() {
        LocalDate lastSunday = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue());
        generateReport("WEEKLY", lastSunday);
    }

    public void generateReport(String type, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        if ("WEEKLY".equals(type)) {
            start = date.minusDays(6).atStartOfDay();
        } else if ("MONTHLY".equals(type)) {
            start = date.withDayOfMonth(1).atStartOfDay();
        }

        Map<String, Object> revenue = orderMapper.aggregateRevenue(start, end);
        Map<String, Object> reportData = new LinkedHashMap<>(revenue);
        reportData.put("type", type);
        reportData.put("date", date.toString());
        reportData.put("generatedAt", LocalDateTime.now().toString());

        String content = new com.fasterxml.jackson.databind.ObjectMapper()
            .valueToTree(reportData).toString();

        Report report = new Report();
        report.setReportType(type);
        report.setReportDate(date);
        report.setContent(content);
        report.setGeneratedAt(LocalDateTime.now());
        reportMapper.insert(report);
        log.info("报表生成: type={}, date={}", type, date);
    }

    /** 查询报表列表 */
    public List<Report> list(String type, LocalDate startDate, LocalDate endDate) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Report> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (type != null) wrapper.eq(Report::getReportType, type);
        if (startDate != null) wrapper.ge(Report::getReportDate, startDate);
        if (endDate != null) wrapper.le(Report::getReportDate, endDate);
        wrapper.orderByDesc(Report::getReportDate);
        return reportMapper.selectList(wrapper);
    }
}
