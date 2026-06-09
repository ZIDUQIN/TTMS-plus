package com.ttms.controller;

import com.ttms.dto.ApiResponse;
import com.ttms.service.BoxOfficeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 电影票房控制器
 * 处理票房榜单、大盘数据、影片详情和趋势图请求
 * 支持单日查询和日期范围查询
 * 所有接口需要管理员权限（通过/admin/前缀认证）
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/box-office")
@RequiredArgsConstructor
public class BoxOfficeController {

    private final BoxOfficeService boxOfficeService;

    /**
     * 获取票房排行榜
     * GET /api/admin/box-office/ranking?startDate=2026-06-01&endDate=2026-06-09&type=comprehensive
     * GET /api/admin/box-office/ranking?date=2026-06-08&type=comprehensive  (兼容旧版单日)
     *
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @param date      单日查询日期（可选，兼容旧版）
     * @param type      票房类型: comprehensive(综合票房) / share(分账票房)，默认comprehensive
     * @return 排行榜列表
     */
    @GetMapping("/ranking")
    public ApiResponse<List<Map<String, Object>>> getRanking(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "comprehensive") String type) {
        LocalDate[] range = resolveDateRange(date, startDate, endDate);
        log.info("票房排行榜查询: startDate={}, endDate={}, type={}",
                range[0].format(DateTimeFormatter.ISO_LOCAL_DATE),
                range[1].format(DateTimeFormatter.ISO_LOCAL_DATE), type);

        List<Map<String, Object>> ranking = boxOfficeService.getRanking(range[0], range[1], type);
        return ApiResponse.success(ranking);
    }

    /**
     * 获取大盘数据
     * GET /api/admin/box-office/dashboard?startDate=2026-06-01&endDate=2026-06-09&type=comprehensive
     *
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @param date      单日查询日期（可选，兼容旧版）
     * @param type      票房类型，默认comprehensive
     * @return 大盘统计数据
     */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "comprehensive") String type) {
        LocalDate[] range = resolveDateRange(date, startDate, endDate);
        log.info("大盘数据查询: startDate={}, endDate={}, type={}",
                range[0].format(DateTimeFormatter.ISO_LOCAL_DATE),
                range[1].format(DateTimeFormatter.ISO_LOCAL_DATE), type);

        Map<String, Object> dashboard = boxOfficeService.getDashboard(range[0], range[1], type);
        return ApiResponse.success(dashboard);
    }

    /**
     * 获取指定影片详细票房信息
     * GET /api/admin/box-office/movie/{movieId}?startDate=2026-06-01&endDate=2026-06-09&type=comprehensive
     *
     * @param movieId 影片ID
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @param date      单日查询日期（可选，兼容旧版）
     * @param type      票房类型，默认comprehensive
     * @return 影片详情
     */
    @GetMapping("/movie/{movieId}")
    public ApiResponse<Map<String, Object>> getMovieDetail(
            @PathVariable Long movieId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "comprehensive") String type) {
        LocalDate[] range = resolveDateRange(date, startDate, endDate);
        log.info("影片票房详情查询: movieId={}, startDate={}, endDate={}, type={}", movieId,
                range[0].format(DateTimeFormatter.ISO_LOCAL_DATE),
                range[1].format(DateTimeFormatter.ISO_LOCAL_DATE), type);

        Map<String, Object> detail = boxOfficeService.getMovieDetail(movieId, range[0], range[1], type);
        if (detail == null) {
            return ApiResponse.error(404, "影片不存在");
        }
        return ApiResponse.success(detail);
    }

    /**
     * 获取指定影片近N日票房趋势
     * GET /api/admin/box-office/movie/{movieId}/trend?date=2026-06-09&type=comprehensive&days=5
     *
     * @param movieId 影片ID
     * @param date    查询截止日期（可选，默认今天）
     * @param type    票房类型，默认comprehensive
     * @param days    统计天数（默认7天）
     * @return 趋势数据列表
     */
    @GetMapping("/movie/{movieId}/trend")
    public ApiResponse<List<Map<String, Object>>> getMovieTrend(
            @PathVariable Long movieId,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "comprehensive") String type,
            @RequestParam(defaultValue = "7") int days) {
        LocalDate endDate = parseDate(date);
        log.info("影片票房趋势查询: movieId={}, endDate={}, type={}, days={}", movieId,
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE), type, days);

        List<Map<String, Object>> trend = boxOfficeService.getMovieTrend(movieId, endDate, type, days);
        return ApiResponse.success(trend);
    }

    /**
     * 解析日期范围参数
     * 优先级：date > (startDate, endDate) > 默认今天
     * - 仅传 date：单日查询（兼容旧版）
     * - 传 startDate + endDate：日期范围查询
     * - 仅传 startDate：从startDate到今天
     * - 都不传：仅今天
     */
    private LocalDate[] resolveDateRange(String date, String startDate, String endDate) {
        LocalDate today = LocalDate.now();

        // 兼容旧版单日参数
        if (date != null && !date.isEmpty()) {
            LocalDate d = parseDate(date);
            return new LocalDate[]{d, d};
        }

        LocalDate start = (startDate != null && !startDate.isEmpty()) ? parseDate(startDate) : today;
        LocalDate end = (endDate != null && !endDate.isEmpty()) ? parseDate(endDate) : start;

        // 保证start <= end
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        return new LocalDate[]{start, end};
    }

    /**
     * 解析日期参数
     * 空值或无效格式返回今天
     */
    private LocalDate parseDate(String date) {
        if (date != null && !date.isEmpty()) {
            try {
                return LocalDate.parse(date);
            } catch (Exception e) {
                log.warn("日期解析失败: {}, 使用今天", date);
            }
        }
        return LocalDate.now();
    }
}
