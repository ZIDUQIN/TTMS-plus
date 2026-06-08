package com.ttms.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 电影票房服务接口
 * 负责票房榜单、大盘数据、影片详情、趋势图数据查询
 */
public interface BoxOfficeService {

    /**
     * 获取票房排行榜（综合票房/分账票房）
     *
     * @param date 查询日期
     * @param type 票房类型: comprehensive(综合票房) / share(分账票房)
     * @return 排行榜列表，每项包含影片信息和票房数据
     */
    List<Map<String, Object>> getRanking(LocalDate date, String type);

    /**
     * 获取大盘数据（总票房、总出票、总场次）
     *
     * @param date 查询日期
     * @param type 票房类型: comprehensive / share
     * @return 大盘统计数据
     */
    Map<String, Object> getDashboard(LocalDate date, String type);

    /**
     * 获取指定影片的详细票房信息
     *
     * @param movieId 影片ID
     * @param date    查询日期
     * @param type    票房类型: comprehensive / share
     * @return 影片详情数据
     */
    Map<String, Object> getMovieDetail(Long movieId, LocalDate date, String type);

    /**
     * 获取指定影片近5日票房趋势数据
     *
     * @param movieId 影片ID
     * @param date    查询日期（作为"今日"）
     * @param type    票房类型: comprehensive / share
     * @return 趋势数据列表，每项包含 date 和 revenue
     */
    List<Map<String, Object>> getMovieTrend(Long movieId, LocalDate date, String type);
}
