package com.ttms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 订单数据访问层
 * 继承MyBatis-Plus的BaseMapper，自动获得CRUD方法
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 根据用户ID查询订单列表
     * 关联影片、影厅信息，按创建时间倒序
     *
     * @param userId 用户ID
     * @return 用户的所有订单（含关联信息）
     */
    @Select("SELECT o.*, m.movie_name, h.hall_name, s.start_time, s.end_time " +
            "FROM `order` o " +
            "LEFT JOIN movie m ON o.movie_id = m.id " +
            "LEFT JOIN hall h ON o.hall_id = h.id " +
            "LEFT JOIN schedule s ON o.schedule_id = s.id " +
            "WHERE o.user_id = #{userId} " +
            "ORDER BY o.create_time DESC")
    List<Order> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据订单号查询订单
     * 订单号是唯一索引，用于精确查找
     *
     * @param orderNo 订单号（格式: yyyyMMdd + 8位随机字符）
     * @return 订单实体
     */
    @Select("SELECT o.*, m.movie_name, h.hall_name, s.start_time, s.end_time " +
            "FROM `order` o " +
            "LEFT JOIN movie m ON o.movie_id = m.id " +
            "LEFT JOIN hall h ON o.hall_id = h.id " +
            "LEFT JOIN schedule s ON o.schedule_id = s.id " +
            "WHERE o.order_no = #{orderNo}")
    Order selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询所有订单（管理端）
     * 关联用户、影片、影厅、场次信息，按创建时间倒序
     *
     * @return 所有订单列表
     */
    @Select("SELECT o.*, u.username, m.movie_name, h.hall_name, s.start_time, s.end_time " +
            "FROM `order` o " +
            "LEFT JOIN user u ON o.user_id = u.id " +
            "LEFT JOIN movie m ON o.movie_id = m.id " +
            "LEFT JOIN hall h ON o.hall_id = h.id " +
            "LEFT JOIN schedule s ON o.schedule_id = s.id " +
            "ORDER BY o.create_time DESC")
    List<Order> selectAllOrders();

    /**
     * 查询过期未支付的订单
     * 状态为0（待支付）且创建时间超过指定分钟数的订单
     *
     * @param timeoutMinutes 超时分钟数
     * @return 过期未支付订单列表
     */
    @Select("SELECT * FROM `order` WHERE status = 0 " +
            "AND TIMESTAMPDIFF(MINUTE, create_time, NOW()) > #{timeoutMinutes}")
    List<Order> selectExpiredOrders(@Param("timeoutMinutes") int timeoutMinutes);

    /**
     * 乐观锁取消订单：仅当status=0（待支付）时才更新为status=5（已过期）
     * 防止支付竞态条件——如果用户在临界点完成支付，此UPDATE不会影响任何行
     *
     * @param orderId 订单ID
     * @return 影响行数（0=已被支付，1=取消成功）
     */
    @Update("UPDATE `order` SET status = 5, update_time = NOW() WHERE id = #{orderId} AND status = 0")
    int cancelIfUnpaid(@Param("orderId") Long orderId);

    /**
     * 按场次日期查询所有有效订单（含影片关联信息）
     * 用于票房计算——在Java层进行聚合
     *
     * @param date 场次日期（从schedule.start_time取）
     * @return 订单列表（含movieName, genre, posterUrl, releaseDate非数据库字段）
     */
    @Select("SELECT o.*, m.movie_name, m.genre, m.poster_url, m.release_date " +
            "FROM `order` o " +
            "JOIN schedule s ON o.schedule_id = s.id " +
            "JOIN movie m ON o.movie_id = m.id " +
            "WHERE o.status IN (1, 2) AND DATE(s.start_time) = #{date}")
    List<Order> selectByScheduleDate(@Param("date") String date);

    /**
     * 按场次日期范围查询所有有效订单（含影片关联信息）
     * 用于票房计算——支持单日和日期范围
     *
     * @param startDate 开始日期（含）YYYY-MM-DD
     * @param endDate   结束日期（含）YYYY-MM-DD
     * @return 订单列表（含movieName, genre, posterUrl, releaseDate非数据库字段）
     */
    @Select("SELECT o.*, m.movie_name, m.genre, m.poster_url, m.release_date " +
            "FROM `order` o " +
            "JOIN schedule s ON o.schedule_id = s.id " +
            "JOIN movie m ON o.movie_id = m.id " +
            "WHERE o.status IN (1, 2) AND DATE(s.start_time) BETWEEN #{startDate} AND #{endDate}")
    List<Order> selectByScheduleDateRange(@Param("startDate") String startDate,
                                           @Param("endDate") String endDate);

    /**
     * 查询影片所有有效订单（用于累计票房计算）
     *
     * @param movieId 影片ID
     * @return 订单列表
     */
    @Select("SELECT o.* " +
            "FROM `order` o " +
            "JOIN schedule s ON o.schedule_id = s.id " +
            "WHERE o.status IN (1, 2) AND o.movie_id = #{movieId}")
    List<Order> selectByMovieIdAllTime(@Param("movieId") Long movieId);

    // ========== 统计聚合查询（避免全量加载OOM） ==========

    /**
     * 聚合营收统计：SUM/COUNT直接在数据库层完成
     */
    @Select("SELECT COALESCE(SUM(o.total_price), 0) as totalRevenue, " +
            "COUNT(*) as orderCount, " +
            "COALESCE(SUM(o.seat_count), 0) as ticketCount " +
            "FROM `order` o " +
            "WHERE o.status IN (1, 2) " +
            "AND o.pay_time >= #{startTime} AND o.pay_time < #{endTime}")
    java.util.Map<String, Object> aggregateRevenue(@Param("startTime") java.time.LocalDateTime startTime,
                                                    @Param("endTime") java.time.LocalDateTime endTime);

    /**
     * 按影片ID聚合票房排行（数据库层聚合，避免全量加载到JVM）
     */
    @Select("SELECT o.movie_id as movieId, " +
            "COALESCE(SUM(o.total_price), 0) as revenue, " +
            "COALESCE(SUM(o.seat_count), 0) as ticketCount " +
            "FROM `order` o " +
            "WHERE o.status IN (1, 2) " +
            "GROUP BY o.movie_id " +
            "ORDER BY revenue DESC")
    List<java.util.Map<String, Object>> aggregateByMovie();

    /**
     * 按日期聚合每日营收
     */
    @Select("SELECT DATE(o.pay_time) as date, " +
            "COALESCE(SUM(o.total_price), 0) as revenue, " +
            "COUNT(*) as orderCount " +
            "FROM `order` o " +
            "WHERE o.status IN (1, 2) " +
            "AND o.pay_time >= #{startTime} AND o.pay_time < #{endTime} " +
            "GROUP BY DATE(o.pay_time) " +
            "ORDER BY date ASC")
    List<java.util.Map<String, Object>> aggregateDailyRevenue(@Param("startTime") java.time.LocalDateTime startTime,
                                                               @Param("endTime") java.time.LocalDateTime endTime);

    /**
     * 按月份聚合月度营收
     */
    @Select("SELECT DATE_FORMAT(o.create_time, '%Y-%m') as month, " +
            "COALESCE(SUM(o.total_price), 0) as revenue, " +
            "COUNT(*) as orderCount, " +
            "COALESCE(SUM(o.seat_count), 0) as ticketCount " +
            "FROM `order` o " +
            "WHERE o.status IN (1, 2) " +
            "AND o.create_time >= #{startTime} AND o.create_time < #{endTime} " +
            "GROUP BY DATE_FORMAT(o.create_time, '%Y-%m') " +
            "ORDER BY month ASC")
    List<java.util.Map<String, Object>> aggregateMonthly(@Param("startTime") java.time.LocalDateTime startTime,
                                                          @Param("endTime") java.time.LocalDateTime endTime);

    /**
     * 按影片和日期聚合票房趋势（单条SQL替代N+1查询）
     */
    @Select("SELECT DATE(s.start_time) as date, " +
            "COALESCE(SUM(o.total_price), 0) as revenue, " +
            "COALESCE(SUM(o.seat_count), 0) as ticketCount " +
            "FROM `order` o " +
            "JOIN schedule s ON o.schedule_id = s.id " +
            "WHERE o.movie_id = #{movieId} AND o.status IN (1, 2) " +
            "AND DATE(s.start_time) BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY DATE(s.start_time) " +
            "ORDER BY date ASC")
    List<java.util.Map<String, Object>> aggregateMovieTrend(@Param("movieId") Long movieId,
                                                             @Param("startDate") String startDate,
                                                             @Param("endDate") String endDate);
}
