package com.ttms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 场次数据访问层
 * 继承MyBatis-Plus的BaseMapper，自动获得CRUD方法
 */
@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    /**
     * 根据影片ID查询场次列表
     * 按放映开始时间升序排列，只返回正常状态的场次
     *
     * @param movieId 影片ID
     * @return 该影片对应的所有正常场次
     */
    @Select("SELECT s.*, m.movie_name, m.duration, m.poster_url, h.hall_name, h.row_count AS hallRowCount, h.col_count AS hallColCount " +
            "FROM schedule s " +
            "LEFT JOIN movie m ON s.movie_id = m.id " +
            "LEFT JOIN hall h ON s.hall_id = h.id " +
            "WHERE s.movie_id = #{movieId} AND s.start_time > NOW() AND s.status = 1 AND s.deleted = 0 " +
            "ORDER BY s.start_time ASC")
    List<Schedule> selectByMovieId(@Param("movieId") Long movieId);

    /**
     * 根据影厅ID查询场次列表
     * 用于检查影厅时间冲突
     *
     * @param hallId 影厅ID
     * @return 该影厅的所有场次
     */
    @Select("SELECT s.*, m.movie_name, m.duration, h.hall_name " +
            "FROM schedule s " +
            "LEFT JOIN movie m ON s.movie_id = m.id " +
            "LEFT JOIN hall h ON s.hall_id = h.id " +
            "WHERE s.hall_id = #{hallId} AND s.deleted = 0")
    List<Schedule> selectByHallId(@Param("hallId") Long hallId);

    /**
     * 查询即将上映的场次
     * 返回开始时间在当前时间之后且状态为正常的场次，按开始时间升序排列
     * 前端"即将上映"页面使用
     *
     * @return 即将上映的场次列表
     */
    @Select("SELECT s.*, m.movie_name, m.duration, m.poster_url, h.hall_name, h.row_count AS hallRowCount, h.col_count AS hallColCount " +
            "FROM schedule s " +
            "LEFT JOIN movie m ON s.movie_id = m.id " +
            "LEFT JOIN hall h ON s.hall_id = h.id " +
            "WHERE s.start_time > NOW() AND s.status = 1 AND s.deleted = 0 " +
            "ORDER BY s.start_time ASC")
    List<Schedule> selectUpcoming();

    /**
     * 原子增加已售座位数（避免竞态条件）
     * UPDATE schedule SET sold_count = sold_count + #{count} WHERE id = #{id}
     *
     * @param id    场次ID
     * @param count 增加数量（可以为负数来减少）
     * @return 影响行数
     */
    @Update("UPDATE schedule SET sold_count = sold_count + #{count} WHERE id = #{id}")
    int incrementSoldCount(@Param("id") Long id, @Param("count") int count);

    /**
     * 原子减少已售座位数
     * UPDATE schedule SET sold_count = sold_count - #{count} WHERE id = #{id} AND sold_count >= #{count}
     *
     * @param id    场次ID
     * @param count 减少数量
     * @return 影响行数
     */
    @Update("UPDATE schedule SET sold_count = sold_count - #{count} WHERE id = #{id} AND sold_count >= #{count}")
    int decrementSoldCount(@Param("id") Long id, @Param("count") int count);

    /**
     * 按日期查询所有场次（含影厅容量信息）
     * 用于票房排片统计——在Java层进行聚合
     *
     * @param date 查询日期
     * @return 场次列表（含hallRowCount, hallColCount非数据库字段）
     */
    @Select("SELECT s.*, h.row_count AS hallRowCount, h.col_count AS hallColCount " +
            "FROM schedule s " +
            "JOIN hall h ON s.hall_id = h.id " +
            "WHERE DATE(s.start_time) = #{date} AND s.deleted = 0")
    List<Schedule> selectByDateWithHall(@Param("date") String date);

    /**
     * 按日期范围查询所有场次（含影厅容量信息）
     * 用于票房排片统计——支持单日和日期范围
     *
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含）
     * @return 场次列表（含hallRowCount, hallColCount非数据库字段）
     */
    @Select("SELECT s.*, h.row_count AS hallRowCount, h.col_count AS hallColCount " +
            "FROM schedule s " +
            "JOIN hall h ON s.hall_id = h.id " +
            "WHERE DATE(s.start_time) BETWEEN #{startDate} AND #{endDate} AND s.deleted = 0")
    List<Schedule> selectByDateRangeWithHall(@Param("startDate") String startDate,
                                              @Param("endDate") String endDate);
}
