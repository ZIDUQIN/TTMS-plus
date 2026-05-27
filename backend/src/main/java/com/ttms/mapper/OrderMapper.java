package com.ttms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
