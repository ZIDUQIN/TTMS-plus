package com.ttms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ttms.entity.Movie;
import com.ttms.entity.Order;
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
import java.util.stream.Collectors;

/**
 * 统计服务实现类
 * 负责营收统计、影片排行、月度数据汇总、Excel导出等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final OrderMapper orderMapper;
    private final MovieMapper movieMapper;

    /**
     * 获取指定日期范围内的营收数据
     * 统计已支付（状态1）和已完成（状态2）的订单
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 营收统计数据（总营收、订单数、售票数、平均票价）
     */
    @Override
    public Map<String, Object> getRevenue(LocalDate startDate, LocalDate endDate) {
        // 查询指定日期范围内的所有订单
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getStatus, 1, 2); // 已支付和已完成的订单

        // 如果有时间范围限制，添加支付时间条件（营收应按支付时间统计）
        if (startDate != null) {
            wrapper.ge(Order::getPayTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(Order::getPayTime, endDate.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(Order::getPayTime);

        List<Order> orders = orderMapper.selectList(wrapper);

        // 统计数据
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int orderCount = orders.size();
        int ticketCount = 0;

        for (Order order : orders) {
            if (order.getTotalPrice() != null) {
                totalRevenue = totalRevenue.add(order.getTotalPrice());
            }
            if (order.getSeatCount() != null) {
                ticketCount += order.getSeatCount();
            }
        }

        // 计算平均票价
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
     * 按售票数量和票房收入综合排序
     *
     * @param limit 返回前N名
     * @return 排行榜列表
     */
    @Override
    public List<Map<String, Object>> getMovieRanking(int limit) {
        // 查询所有已支付/已完成的订单
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getStatus, 1, 2);
        List<Order> orders = orderMapper.selectList(wrapper);

        // 按影片ID分组统计
        Map<Long, BigDecimal> revenueByMovie = new LinkedHashMap<>();
        Map<Long, Integer> countByMovie = new LinkedHashMap<>();

        for (Order order : orders) {
            Long movieId = order.getMovieId();
            if (movieId != null) {
                // 累计营收
                revenueByMovie.merge(movieId,
                    order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO,
                    BigDecimal::add);
                // 累计售票数
                countByMovie.merge(movieId,
                    order.getSeatCount() != null ? order.getSeatCount() : 0,
                    Integer::sum);
            }
        }

        // 构建排行列表
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : revenueByMovie.entrySet()) {
            Long movieId = entry.getKey();
            Movie movie = movieMapper.selectById(movieId);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("movieId", movieId);
            item.put("movieName", movie != null ? movie.getMovieName() : "未知影片");
            item.put("posterUrl", movie != null ? movie.getPosterUrl() : "");
            item.put("revenue", entry.getValue());
            item.put("ticketCount", countByMovie.getOrDefault(movieId, 0));
            ranking.add(item);
        }

        // 按营收降序排序
        ranking.sort((a, b) -> {
            BigDecimal revA = (BigDecimal) a.get("revenue");
            BigDecimal revB = (BigDecimal) b.get("revenue");
            return revB.compareTo(revA);
        });

        // 限制返回数量
        if (ranking.size() > limit) {
            ranking = ranking.subList(0, limit);
        }

        log.info("影片排行榜查询: 共{}部影片", ranking.size());
        return ranking;
    }

    /**
     * 获取每日营收数据（用于前端趋势图）
     * 按支付时间(日期级别)分组统计，按日期升序排列
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 每日数据列表
     */
    @Override
    public List<Map<String, Object>> getDailyRevenue(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getStatus, 1, 2);
        wrapper.ge(Order::getPayTime, startDate.atStartOfDay());
        wrapper.le(Order::getPayTime, endDate.plusDays(1).atStartOfDay());

        List<Order> orders = orderMapper.selectList(wrapper);

        // 按日期分组
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, List<Order>> grouped = orders.stream()
            .filter(o -> o.getPayTime() != null)
            .collect(Collectors.groupingBy(
                o -> o.getPayTime().format(dateFormatter),
                LinkedHashMap::new,
                Collectors.toList()));

        // 构建完整的日期范围（包括没有订单的日期）
        List<Map<String, Object>> dailyData = new ArrayList<>();
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateKey = date.format(dateFormatter);
            List<Order> dayOrders = grouped.getOrDefault(dateKey, List.of());

            BigDecimal dayRevenue = BigDecimal.ZERO;
            int dayOrderCount = dayOrders.size();
            for (Order o : dayOrders) {
                if (o.getTotalPrice() != null) {
                    dayRevenue = dayRevenue.add(o.getTotalPrice());
                }
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", dateKey);
            item.put("revenue", dayRevenue);
            item.put("orderCount", dayOrderCount);
            dailyData.add(item);
        }

        log.info("每日营收统计: {} ~ {} -> {} 天", startDate, endDate, dailyData.size());
        return dailyData;
    }

    /**
     * 获取最近12个月的月度统计数据
     * 从当前月份往前推12个月，统计每月的营收、订单数、售票数
     *
     * @return 月度数据列表
     */
    @Override
    public List<Map<String, Object>> getMonthlyData() {
        LocalDate now = LocalDate.now();
        // 从12个月前开始
        LocalDate startDate = now.minus(12, ChronoUnit.MONTHS).withDayOfMonth(1);

        // 查询范围内的所有订单
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getStatus, 1, 2);
        wrapper.ge(Order::getCreateTime, startDate.atStartOfDay());
        wrapper.le(Order::getCreateTime, now.plusDays(1).atStartOfDay());
        List<Order> orders = orderMapper.selectList(wrapper);

        // 按月分组（格式: yyyy-MM）
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, List<Order>> groupedByMonth = orders.stream()
            .collect(Collectors.groupingBy(
                o -> o.getCreateTime() != null ? o.getCreateTime().format(monthFormatter) : "未知",
                LinkedHashMap::new,
                Collectors.toList()));

        // 构建完整的12个月数据（包括没有订单的月份）
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minus(i, ChronoUnit.MONTHS);
            String monthKey = month.format(monthFormatter);

            List<Order> monthOrders = groupedByMonth.getOrDefault(monthKey, List.of());

            BigDecimal monthRevenue = BigDecimal.ZERO;
            int monthOrderCount = monthOrders.size();
            int monthTicketCount = 0;

            for (Order order : monthOrders) {
                if (order.getTotalPrice() != null) {
                    monthRevenue = monthRevenue.add(order.getTotalPrice());
                }
                if (order.getSeatCount() != null) {
                    monthTicketCount += order.getSeatCount();
                }
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", monthKey);
            item.put("monthName", month.getMonthValue() + "月");
            item.put("year", month.getYear());
            item.put("revenue", monthRevenue);
            item.put("orderCount", monthOrderCount);
            item.put("ticketCount", monthTicketCount);
            monthlyData.add(item);
        }

        log.info("月度数据统计: 共{}个月有订单数据", groupedByMonth.size());
        return monthlyData;
    }

    /**
     * 导出统计报表为Excel文件
     * 使用Apache POI生成xlsx格式文件
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return Excel文件的相对路径
     */
    @Override
    public String exportToExcel(LocalDate startDate, LocalDate endDate) {
        // 获取数据
        Map<String, Object> revenue = getRevenue(startDate, endDate);
        List<Map<String, Object>> ranking = getMovieRanking(10);
        List<Map<String, Object>> monthly = getMonthlyData();

        // 创建工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            // ===== Sheet1: 营收概览 =====
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
            // 自动调整列宽
            sheet1.setColumnWidth(0, 20 * 256);
            sheet1.setColumnWidth(1, 15 * 256);

            // ===== Sheet2: 影片票房排行 =====
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
                row.createCell(1).setCellValue(item.get("movieName").toString());
                row.createCell(2).setCellValue(item.get("revenue").toString());
                row.createCell(3).setCellValue(item.get("ticketCount").toString());
            }
            sheet2.setColumnWidth(0, 10 * 256);
            sheet2.setColumnWidth(1, 30 * 256);
            sheet2.setColumnWidth(2, 15 * 256);
            sheet2.setColumnWidth(3, 12 * 256);

            // ===== Sheet3: 月度趋势 =====
            Sheet sheet3 = workbook.createSheet("月度趋势");
            Row header3 = sheet3.createRow(0);
            header3.createCell(0).setCellValue("月份");
            header3.createCell(1).setCellValue("营收(元)");
            header3.createCell(2).setCellValue("订单数");
            header3.createCell(3).setCellValue("售票数");

            for (int i = 0; i < monthly.size(); i++) {
                Row row = sheet3.createRow(i + 1);
                Map<String, Object> item = monthly.get(i);
                row.createCell(0).setCellValue(item.get("month").toString());
                row.createCell(1).setCellValue(item.get("revenue").toString());
                row.createCell(2).setCellValue(item.get("orderCount").toString());
                row.createCell(3).setCellValue(item.get("ticketCount").toString());
            }
            sheet3.setColumnWidth(0, 15 * 256);
            sheet3.setColumnWidth(1, 15 * 256);
            sheet3.setColumnWidth(2, 12 * 256);
            sheet3.setColumnWidth(3, 12 * 256);

            // 确保上传目录存在
            String uploadDir = "./uploads/reports/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成文件名
            String fileName = "statistics_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "_" + System.currentTimeMillis() + ".xlsx";
            String filePath = uploadDir + fileName;

            // 写入文件
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
}
