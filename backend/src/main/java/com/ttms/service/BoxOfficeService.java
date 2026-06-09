package com.ttms.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 电影票房服务接口
 * 负责票房榜单、大盘数据、影片详情、趋势图数据查询
 * 支持单日查询和日期范围查询
 */
public interface BoxOfficeService {

    /**
     * 获取票房排行榜（综合票房/分账票房）
     * 支持单日和日期范围查询
     *
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含），与startDate相同时表示单日查询
     * @param type      票房类型: comprehensive(综合票房) / share(分账票房)
     * @return 排行榜列表，每项包含影片信息和票房数据
     */
    List<Map<String, Object>> getRanking(LocalDate startDate, LocalDate endDate, String type);

    /**
     * 获取大盘数据（总票房、总出票、总场次）
     * 支持单日和日期范围查询
     *
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含），与startDate相同时表示单日查询
     * @param type      票房类型: comprehensive / share
     * @return 大盘统计数据
     */
    Map<String, Object> getDashboard(LocalDate startDate, LocalDate endDate, String type);

    /**
     * 获取指定影片的详细票房信息
     * 支持单日和日期范围查询
     *
     * @param movieId   影片ID
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含），与startDate相同时表示单日查询
     * @param type      票房类型: comprehensive / share
     * @return 影片详情数据
     */
    Map<String, Object> getMovieDetail(Long movieId, LocalDate startDate, LocalDate endDate, String type);

    /**
     * 获取指定影片近N日票房趋势数据
     *
     * @param movieId   影片ID
     * @param endDate   查询截止日期（作为"今日"）
     * @param type      票房类型: comprehensive / share
     * @param days      向前推的天数（含endDate当天）
     * @return 趋势数据列表，每项包含 date 和 revenue
     */
    List<Map<String, Object>> getMovieTrend(Long movieId, LocalDate endDate, String type, int days);
}
