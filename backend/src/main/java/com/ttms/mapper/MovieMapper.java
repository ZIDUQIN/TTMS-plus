package com.ttms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.Movie;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 影片数据访问层
 * 继承MyBatis-Plus的BaseMapper，自动获得CRUD方法
 */
@Mapper
public interface MovieMapper extends BaseMapper<Movie> {

    /**
     * 查询热门影片列表
     * 按热度(isHot)降序、排序权重(sort_order)降序排列
     * 前端首页展示使用
     *
     * @return 热门影片列表
     */
    @Select("SELECT * FROM movie WHERE status = 1 AND is_hot = 1 AND deleted = 0 ORDER BY sort_order DESC")
    List<Movie> selectHotMovies();

    /**
     * 根据关键词搜索影片
     * 支持按片名、导演、主演模糊匹配
     *
     * @param keyword 搜索关键词
     * @return 匹配的影片列表
     */
    @Select("SELECT * FROM movie WHERE deleted = 0 AND " +
            "(movie_name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR director LIKE CONCAT('%', #{keyword}, '%') " +
            "OR actors LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY sort_order DESC, create_time DESC")
    List<Movie> searchMovies(@Param("keyword") String keyword);
}
