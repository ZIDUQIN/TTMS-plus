package com.ttms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 座位数据访问层
 * 继承MyBatis-Plus的BaseMapper，自动获得CRUD方法
 */
@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

    /**
     * 根据场次ID查询所有座位
     * 按行号、列号排序，便于前端展示座位矩阵
     *
     * @param scheduleId 场次ID
     * @return 该场次的座位列表
     */
    @Select("SELECT * FROM seat WHERE schedule_id = #{scheduleId} ORDER BY seat_row ASC, seat_col ASC")
    List<Seat> selectByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * 根据座位编号和场次ID查询座位
     * 用于创建订单时校验座位是否存在
     *
     * @param scheduleId 场次ID
     * @param seatNumber 座位编号（如"A-05"）
     * @return 座位实体
     */
    @Select("SELECT * FROM seat WHERE schedule_id = #{scheduleId} AND seat_number = #{seatNumber}")
    Seat selectByScheduleAndNumber(@Param("scheduleId") Long scheduleId,
                                   @Param("seatNumber") String seatNumber);

    /**
     * 锁定座位
     * 将指定座位状态设置为1（已锁定），并记录锁定时间
     * 用于创建订单时预占座位
     *
     * @param seatId   座位ID
     * @param orderId  关联订单ID
     * @return 影响行数
     */
    @Update("UPDATE seat SET status = 1, lock_time = NOW(), order_id = #{orderId} " +
            "WHERE id = #{seatId} AND status = 0")
    int lockSeat(@Param("seatId") Long seatId, @Param("orderId") Long orderId);

    /**
     * 释放座位
     * 将座位状态恢复为0（空闲），清除锁定时间和订单关联
     * 用于订单取消、退款或改签时释放座位
     *
     * @param seatId 座位ID
     * @return 影响行数
     */
    @Update("UPDATE seat SET status = 0, lock_time = NULL, order_id = NULL WHERE id = #{seatId}")
    int releaseSeat(@Param("seatId") Long seatId);

    /**
     * 将座位标记为已售出（按场次和座位号）
     * 支付完成后将座位状态改为2（已售出）
     *
     * @param scheduleId 场次ID
     * @param seatNumber 座位编号
     * @return 影响行数
     */
    @Update("UPDATE seat SET status = 2 WHERE schedule_id = #{scheduleId} AND seat_number = #{seatNumber} AND status = 0")
    int markSoldByScheduleAndNumber(@Param("scheduleId") Long scheduleId,
                                    @Param("seatNumber") String seatNumber);

    /**
     * 将座位标记为已售出
     * 支付完成后将座位状态改为2（已售出）
     *
     * @param seatId 座位ID
     * @return 影响行数
     */
    @Update("UPDATE seat SET status = 2 WHERE id = #{seatId}")
    int markSold(@Param("seatId") Long seatId);

    /**
     * 批量释放指定订单关联的所有座位
     * 用于取消订单时批量释放座位
     *
     * @param orderId 订单ID
     * @return 影响行数
     */
    @Update("UPDATE seat SET status = 0, lock_time = NULL, order_id = NULL WHERE order_id = #{orderId}")
    int releaseSeatsByOrderId(@Param("orderId") Long orderId);

    /**
     * 乐观锁释放座位：只释放status=1（已锁定）的座位
     * 防止支付竞态条件——如果用户刚支付完成（座位status已变为2），此UPDATE不会影响任何行
     *
     * @param orderId 订单ID
     * @return 影响行数
     */
    @Update("UPDATE seat SET status = 0, lock_time = NULL, order_id = NULL " +
            "WHERE order_id = #{orderId} AND status = 1")
    int releaseSeatsByOrderIdOptimistic(@Param("orderId") Long orderId);

    /**
     * 批量查询各场次的已锁定座位数（一次GROUP BY替代N次COUNT）
     *
     * @param scheduleIds 场次ID列表
     * @return [{scheduleId, cnt}, ...]
     */
    @Select("<script>" +
            "SELECT schedule_id as scheduleId, COUNT(*) as cnt " +
            "FROM seat WHERE status = 1 AND schedule_id IN " +
            "<foreach collection='scheduleIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " GROUP BY schedule_id" +
            "</script>")
    List<java.util.Map<String, Object>> countLockedByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);

    /**
     * 释放超时锁定的座位（兜底机制）
     * 释放 lock_time 超过指定分钟数且 status=1 的座位
     *
     * @param timeoutMinutes 超时分钟数
     * @return 影响行数
     */
    @Update("UPDATE seat SET status = 0, lock_time = NULL, order_id = NULL " +
            "WHERE status = 1 AND lock_time < DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    int releaseStaleLockedSeats(@Param("timeoutMinutes") int timeoutMinutes);

    /**
     * 批量插入座位（用于自动生成场次座位）
     * 注意：此方法需配合XML Mapper使用，或在Service层逐条插入
     */
    @Update("DELETE FROM seat WHERE schedule_id = #{scheduleId}")
    int deleteByScheduleId(@Param("scheduleId") Long scheduleId);
}
