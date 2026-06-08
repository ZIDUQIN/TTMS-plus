package com.ttms.controller;

import com.ttms.dto.ApiResponse;
import com.ttms.service.BoxOfficeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 电影票房控制器
 * 处理票房榜单、大盘数据、影片详情和趋势图请求
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
     * GET /api/admin/box-office/ranking?date=2026-06-08&type=comprehensive
     *
     * @param date 查询日期（可选，默认今天）
     * @param type 票房类型: comprehensive(综合票房) / share(分账票房)，默认comprehensive
     * @return 排行榜列表
     */
    @GetMapping("/ranking")
    public ApiResponse<List<Map<String, Object>>> getRanking(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "comprehensive") String type) {
        log.info("票房排行榜查询: date={}, type={}", date, type);

        LocalDate queryDate = parseDate(date);
        List<Map<String, Object>> ranking = boxOfficeService.getRanking(queryDate, type);
        return ApiResponse.success(ranking);
    }

    /**
     * 获取大盘数据
     * GET /api/admin/box-office/dashboard?date=2026-06-08&type=comprehensive
     *
     * @param date 查询日期（可选，默认今天）
     * @param type 票房类型，默认comprehensive
     * @return 大盘统计数据
     */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "comprehensive") String type) {
        log.info("大盘数据查询: date={}, type={}", date, type);

        LocalDate queryDate = parseDate(date);
        Map<String, Object> dashboard = boxOfficeService.getDashboard(queryDate, type);
        return ApiResponse.success(dashboard);
    }

    /**
     * 获取指定影片详细票房信息
     * GET /api/admin/box-office/movie/{movieId}?date=2026-06-08&type=comprehensive
     *
     * @param movieId 影片ID
     * @param date    查询日期（可选，默认今天）
     * @param type    票房类型，默认comprehensive
     * @return 影片详情
     */
    @GetMapping("/movie/{movieId}")
    public ApiResponse<Map<String, Object>> getMovieDetail(
            @PathVariable Long movieId,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "comprehensive") String type) {
        log.info("影片票房详情查询: movieId={}, date={}, type={}", movieId, date, type);

        LocalDate queryDate = parseDate(date);
        Map<String, Object> detail = boxOfficeService.getMovieDetail(movieId, queryDate, type);
        if (detail == null) {
            return ApiResponse.error(404, "影片不存在");
        }
        return ApiResponse.success(detail);
    }

    /**
     * 获取指定影片近5日票房趋势
     * GET /api/admin/box-office/movie/{movieId}/trend?date=2026-06-08&type=comprehensive
     *
     * @param movieId 影片ID
     * @param date    查询日期（作为"今日"基准，可选）
     * @param type    票房类型，默认comprehensive
     * @return 趋势数据列表
     */
    @GetMapping("/movie/{movieId}/trend")
    public ApiResponse<List<Map<String, Object>>> getMovieTrend(
            @PathVariable Long movieId,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "comprehensive") String type) {
        log.info("影片票房趋势查询: movieId={}, date={}, type={}", movieId, date, type);

        LocalDate queryDate = parseDate(date);
        List<Map<String, Object>> trend = boxOfficeService.getMovieTrend(movieId, queryDate, type);
        return ApiResponse.success(trend);
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
