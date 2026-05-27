package com.ttms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
            "WHERE s.movie_id = #{movieId} AND s.deleted = 0 " +
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
}
