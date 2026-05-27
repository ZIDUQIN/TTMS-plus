package com.ttms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.entity.Movie;

import java.util.List;

/**
 * 影片服务接口
 * 负责影片的增删改查、搜索、热门设置等业务
 */
public interface MovieService {

    /**
     * 分页查询影片列表
     *
     * @param page   页码
     * @param size   每页大小
     * @param status 状态筛选（null表示查询全部）
     * @return 分页结果
     */
    Page<Movie> list(int page, int size, Integer status);

    /**
     * 查询影片详情
     *
     * @param id 影片ID
     * @return 影片实体
     */
    Movie detail(Long id);

    /**
     * 添加影片
     *
     * @param movie 影片信息
     * @return 添加后的影片（含生成的ID）
     */
    Movie add(Movie movie);

    /**
     * 更新影片信息
     *
     * @param movie 影片信息（含ID）
     * @return 更新后的影片
     */
    Movie update(Movie movie);

    /**
     * 删除影片（逻辑删除）
     *
     * @param id 影片ID
     */
    void delete(Long id);

    /**
     * 搜索影片
     * 支持按片名、导演、主演模糊搜索
     *
     * @param keyword 搜索关键词
     * @return 匹配的影片列表
     */
    List<Movie> search(String keyword);

    /**
     * 设置影片热门状态
     *
     * @param id   影片ID
     * @param isHot 是否热门（0-否 1-是）
     */
    void setHot(Long id, Integer isHot);

    /**
     * 设置影片状态（上架/下架/即将上映）
     *
     * @param id     影片ID
     * @param status 状态值（0-下架 1-上架 2-即将上映）
     */
    void setStatus(Long id, Integer status);

    /**
     * 查询热门影片
     *
     * @return 热门影片列表
     */
    List<Movie> getHotMovies();
}
