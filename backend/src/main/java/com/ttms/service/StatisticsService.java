package com.ttms.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计服务接口
 * 负责营收统计、影片排行、月度数据汇总、Excel导出等
 */
public interface StatisticsService {

    /**
     * 获取指定日期范围内的营收数据
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 营收统计数据
     *         key: "totalRevenue" - 总营收
     *         key: "orderCount"   - 订单总数
     *         key: "ticketCount"  - 售票总张数
     *         key: "avgPrice"     - 平均票价
     */
    Map<String, Object> getRevenue(LocalDate startDate, LocalDate endDate);

    /**
     * 获取影片票房排行榜
     * 按售票数量和票房收入排序
     *
     * @param limit 返回前N名
     * @return 排行榜列表，每项包含影片信息和销售数据
     */
    List<Map<String, Object>> getMovieRanking(int limit);

    /**
     * 获取最近12个月的月度统计数据
     * 包含每月营收、订单数、售票数
     *
     * @return 月度数据列表
     */
    List<Map<String, Object>> getMonthlyData();

    /**
     * 导出统计报表为Excel
     * 返回Excel文件路径或Base64编码内容
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return Excel文件路径
     */
    String exportToExcel(LocalDate startDate, LocalDate endDate);

    /**
     * 获取每日营收数据（用于前端趋势图）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 每日数据列表，每项包含 date, revenue, orderCount
     */
    List<Map<String, Object>> getDailyRevenue(LocalDate startDate, LocalDate endDate);
}
