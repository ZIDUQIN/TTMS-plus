package com.ttms.service.impl;

import com.ttms.entity.Movie;
import com.ttms.mapper.MovieMapper;
import com.ttms.mapper.OrderMapper;
import com.ttms.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 统计服务实现类
 * 负责营收统计、影片排行、月度数据汇总、Excel导出等
 * 所有聚合计算在数据库层完成，避免全量加载订单到JVM导致OOM
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final OrderMapper orderMapper;
    private final MovieMapper movieMapper;

    /**
     * 获取指定日期范围内的营收数据
     * 使用数据库SUM/COUNT聚合，单次SQL返回结果
     */
    @Override
    public Map<String, Object> getRevenue(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate != null ? startDate.atStartOfDay()
            : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime endTime = endDate != null ? endDate.plusDays(1).atStartOfDay()
            : LocalDate.now().plusDays(1).atStartOfDay();

        Map<String, Object> agg = orderMapper.aggregateRevenue(startTime, endTime);

        BigDecimal totalRevenue = toBigDecimal(agg.get("totalRevenue"));
        long orderCount = toLong(agg.get("orderCount"));
        long ticketCount = toLong(agg.get("ticketCount"));

        BigDecimal avgPrice = ticketCount > 0
            ? totalRevenue.divide(BigDecimal.valueOf(ticketCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRevenue", totalRevenue);
        result.put("orderCount", orderCount);
        result.put("ticketCount", ticketCount);
        result.put("avgPrice", avgPrice);

        log.info("营收统计: {} ~ {} -> 总营收={}, 订单数={}, 售票数={}",
            startDate, endDate, totalRevenue, orderCount, ticketCount);
        return result;
    }

    /**
     * 获取影片票房排行榜
     * 使用数据库GROUP BY聚合，单次SQL返回所有影片排行
     */
    @Override
    public List<Map<String, Object>> getMovieRanking(int limit) {
        List<Map<String, Object>> rows = orderMapper.aggregateByMovie();

        List<Map<String, Object>> ranking = new ArrayList<>();
        int count = 0;
        for (Map<String, Object> row : rows) {
            if (count >= limit) break;
            Long movieId = toLong(row.get("movieId"));
            Movie movie = movieId != null ? movieMapper.selectById(movieId) : null;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("movieId", movieId);
            item.put("movieName", movie != null ? movie.getMovieName() : "未知影片");
            item.put("posterUrl", movie != null ? movie.getPosterUrl() : "");
            item.put("revenue", toBigDecimal(row.get("revenue")));
            item.put("ticketCount", ((Number) row.get("ticketCount")).intValue());
            ranking.add(item);
            count++;
        }

        log.info("影片排行榜查询: 共{}部影片", ranking.size());
        return ranking;
    }

    /**
     * 获取每日营收数据（用于前端趋势图）
     * 使用数据库GROUP BY DATE聚合
     */
    @Override
    public List<Map<String, Object>> getDailyRevenue(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();

        List<Map<String, Object>> rows = orderMapper.aggregateDailyRevenue(startTime, endTime);

        // 构建完整的日期范围（包括没有订单的日期）
        Map<String, Map<String, Object>> dataMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String dateKey = row.get("date").toString();
            dataMap.put(dateKey, row);
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Map<String, Object>> dailyData = new ArrayList<>();
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateKey = date.format(dateFormatter);
            Map<String, Object> row = dataMap.get(dateKey);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", dateKey);
            if (row != null) {
                item.put("revenue", toBigDecimal(row.get("revenue")));
                item.put("orderCount", ((Number) row.get("orderCount")).intValue());
            } else {
                item.put("revenue", BigDecimal.ZERO);
                item.put("orderCount", 0);
            }
            dailyData.add(item);
        }

        return dailyData;
    }

    /**
     * 获取最近12个月的月度统计数据
     * 使用数据库GROUP BY DATE_FORMAT聚合
     */
    @Override
    public List<Map<String, Object>> getMonthlyData() {
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.minus(12, ChronoUnit.MONTHS).withDayOfMonth(1);

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = now.plusDays(1).atStartOfDay();

        List<Map<String, Object>> rows = orderMapper.aggregateMonthly(startTime, endTime);

        Map<String, Map<String, Object>> dataMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String monthKey = row.get("month").toString();
            dataMap.put(monthKey, row);
        }

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minus(i, ChronoUnit.MONTHS);
            String monthKey = month.format(monthFormatter);
            Map<String, Object> row = dataMap.get(monthKey);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", monthKey);
            item.put("monthName", month.getMonthValue() + "月");
            item.put("year", month.getYear());
            if (row != null) {
                item.put("revenue", toBigDecimal(row.get("revenue")));
                item.put("orderCount", ((Number) row.get("orderCount")).intValue());
                item.put("ticketCount", ((Number) row.get("ticketCount")).intValue());
            } else {
                item.put("revenue", BigDecimal.ZERO);
                item.put("orderCount", 0);
                item.put("ticketCount", 0);
            }
            monthlyData.add(item);
        }

        log.info("月度数据统计完成");
        return monthlyData;
    }

    @Override
    public String exportToExcel(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> revenue = getRevenue(startDate, endDate);
        List<Map<String, Object>> ranking = getMovieRanking(10);
        List<Map<String, Object>> monthly = getMonthlyData();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet1 = workbook.createSheet("营收概览");
            Row header1 = sheet1.createRow(0);
            header1.createCell(0).setCellValue("统计项");
            header1.createCell(1).setCellValue("数值");

            String[][] overviewData = {
                {"统计开始日期", startDate != null ? startDate.toString() : "不限"},
                {"统计结束日期", endDate != null ? endDate.toString() : "不限"},
                {"总营收(元)", revenue.get("totalRevenue").toString()},
                {"订单总数", revenue.get("orderCount").toString()},
                {"售票总张数", revenue.get("ticketCount").toString()},
                {"平均票价(元)", revenue.get("avgPrice").toString()},
            };
            for (int i = 0; i < overviewData.length; i++) {
                Row row = sheet1.createRow(i + 1);
                row.createCell(0).setCellValue(overviewData[i][0]);
                row.createCell(1).setCellValue(overviewData[i][1]);
            }
            sheet1.setColumnWidth(0, 20 * 256);
            sheet1.setColumnWidth(1, 15 * 256);

            Sheet sheet2 = workbook.createSheet("影片票房排行");
            Row header2 = sheet2.createRow(0);
            header2.createCell(0).setCellValue("排名");
            header2.createCell(1).setCellValue("影片名称");
            header2.createCell(2).setCellValue("票房收入(元)");
            header2.createCell(3).setCellValue("售票数量");

            for (int i = 0; i < ranking.size(); i++) {
                Row row = sheet2.createRow(i + 1);
                Map<String, Object> item = ranking.get(i);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(String.valueOf(item.get("movieName")));
                row.createCell(2).setCellValue(String.valueOf(item.get("revenue")));
                row.createCell(3).setCellValue(String.valueOf(item.get("ticketCount")));
            }
            sheet2.setColumnWidth(0, 10 * 256);
            sheet2.setColumnWidth(1, 30 * 256);
            sheet2.setColumnWidth(2, 15 * 256);
            sheet2.setColumnWidth(3, 12 * 256);

            Sheet sheet3 = workbook.createSheet("月度趋势");
            Row header3 = sheet3.createRow(0);
            header3.createCell(0).setCellValue("月份");
            header3.createCell(1).setCellValue("营收(元)");
            header3.createCell(2).setCellValue("订单数");
            header3.createCell(3).setCellValue("售票数");

            for (int i = 0; i < monthly.size(); i++) {
                Row row = sheet3.createRow(i + 1);
                Map<String, Object> item = monthly.get(i);
                row.createCell(0).setCellValue(String.valueOf(item.get("month")));
                row.createCell(1).setCellValue(String.valueOf(item.get("revenue")));
                row.createCell(2).setCellValue(String.valueOf(item.get("orderCount")));
                row.createCell(3).setCellValue(String.valueOf(item.get("ticketCount")));
            }
            sheet3.setColumnWidth(0, 15 * 256);
            sheet3.setColumnWidth(1, 15 * 256);
            sheet3.setColumnWidth(2, 12 * 256);
            sheet3.setColumnWidth(3, 12 * 256);

            String uploadDir = "./uploads/reports/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = "statistics_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "_" + System.currentTimeMillis() + ".xlsx";
            String filePath = uploadDir + fileName;

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            log.info("统计报表导出成功: {}", filePath);
            return "/uploads/reports/" + fileName;

        } catch (Exception e) {
            log.error("Excel导出失败", e);
            throw new RuntimeException("Excel导出失败: " + e.getMessage());
        }
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }
}
