package com.ttms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.OrderLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单操作日志数据访问层
 * 继承MyBatis-Plus的BaseMapper，自动获得CRUD方法
 */
@Mapper
public interface OrderLogMapper extends BaseMapper<OrderLog> {

    /**
     * 根据订单ID查询操作日志
     * 按创建时间倒序排列，展示最新操作在前
     *
     * @param orderId 订单ID
     * @return 该订单的操作日志列表
     */
    @Select("SELECT * FROM order_log WHERE order_id = #{orderId} ORDER BY create_time DESC")
    List<OrderLog> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 查询所有操作日志（管理端）
     * 按创建时间倒序排列
     *
     * @return 所有操作日志列表
     */
    @Select("SELECT * FROM order_log ORDER BY create_time DESC")
    List<OrderLog> selectAllLogs();
}
