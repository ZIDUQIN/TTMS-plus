package com.ttms.controller;

import com.ttms.dto.ApiResponse;
import com.ttms.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计控制器
 * 处理营收统计、影片排行、月度数据和Excel导出请求
 * 所有接口需要管理员权限
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 获取营收统计数据
     * GET /api/admin/statistics/revenue?startDate=2024-01-01&endDate=2024-12-31
     *
     * @param startDate 开始日期（可选，默认最近30天）
     * @param endDate   结束日期（可选，默认今天）
     * @return 营收数据：totalRevenue, orderCount, ticketCount, avgPrice
     */
    @GetMapping("/revenue")
    public ApiResponse<Map<String, Object>> getRevenue(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("营收统计查询: startDate={}, endDate={}", startDate, endDate);

        LocalDate start = (startDate != null && !startDate.isEmpty())
            ? LocalDate.parse(startDate)
            : LocalDate.now().minusDays(30);
        LocalDate end = (endDate != null && !endDate.isEmpty())
            ? LocalDate.parse(endDate)
            : LocalDate.now();

        Map<String, Object> data = statisticsService.getRevenue(start, end);
        return ApiResponse.success(data);
    }

    /**
     * 获取影片票房排行榜
     * GET /api/admin/statistics/movie-ranking?limit=10
     *
     * @param limit 返回前N名（默认10）
     * @return 排行榜列表
     */
    @GetMapping("/movie-ranking")
    public ApiResponse<List<Map<String, Object>>> getMovieRanking(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("影片票房排行查询: limit={}", limit);
        List<Map<String, Object>> ranking = statisticsService.getMovieRanking(limit);
        return ApiResponse.success(ranking);
    }

    /**
     * 获取每日营收数据（用于前端趋势图）
     * GET /api/admin/statistics/revenue/daily?startDate=2024-01-01&endDate=2024-12-31
     *
     * @param startDate 开始日期（可选，默认最近30天）
     * @param endDate   结束日期（可选，默认今天）
     * @return 每日数据列表：date, revenue, orderCount
     */
    @GetMapping("/revenue/daily")
    public ApiResponse<List<Map<String, Object>>> getDailyRevenue(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("每日营收统计查询: startDate={}, endDate={}", startDate, endDate);
        LocalDate start = (startDate != null && !startDate.isEmpty())
            ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = (endDate != null && !endDate.isEmpty())
            ? LocalDate.parse(endDate) : LocalDate.now();
        List<Map<String, Object>> data = statisticsService.getDailyRevenue(start, end);
        return ApiResponse.success(data);
    }

    /**
     * 获取月度统计数据
     * GET /api/admin/statistics/monthly
     * 返回最近12个月的月度营收、订单数、售票数
     *
     * @return 月度数据列表
     */
    @GetMapping("/monthly")
    public ApiResponse<List<Map<String, Object>>> getMonthlyData() {
        log.info("月度数据统计查询");
        List<Map<String, Object>> data = statisticsService.getMonthlyData();
        return ApiResponse.success(data);
    }

    /**
     * 导出Excel报表
     * GET /api/admin/statistics/export?startDate=2024-01-01&endDate=2024-12-31
     *
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @return Excel文件下载路径
     */
    @GetMapping("/export")
    public ApiResponse<String> export(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("统计报表导出: startDate={}, endDate={}", startDate, endDate);

        LocalDate start = (startDate != null && !startDate.isEmpty())
            ? LocalDate.parse(startDate)
            : LocalDate.now().minusDays(30);
        LocalDate end = (endDate != null && !endDate.isEmpty())
            ? LocalDate.parse(endDate)
            : LocalDate.now();

        String filePath = statisticsService.exportToExcel(start, end);
        return ApiResponse.success("报表导出成功", filePath);
    }
}
