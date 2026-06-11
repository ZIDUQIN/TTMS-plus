package com.ttms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.entity.Movie;
import com.ttms.exception.BusinessException;
import com.ttms.mapper.MovieMapper;
import com.ttms.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 影片服务实现类
 * 负责影片的增删改查、热门设置、状态管理等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieMapper movieMapper;

    /**
     * 分页查询影片列表
     * 支持按状态筛选，按排序权重降序、创建时间倒序排列
     *
     * @param page   页码
     * @param size   每页大小
     * @param status 状态筛选（null查询全部）
     * @return 分页结果
     */
    @Override
    public Page<Movie> list(int page, int size, Integer status) {
        Page<Movie> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        // 如果指定了状态筛选条件
        if (status != null) {
            wrapper.eq(Movie::getStatus, status);
        }
        wrapper.orderByDesc(Movie::getSortOrder)
               .orderByDesc(Movie::getCreateTime);

        return movieMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 查询影片详情
     *
     * @param id 影片ID
     * @return 影片实体
     */
    @Override
    public Movie detail(Long id) {
        Movie movie = movieMapper.selectById(id);
        if (movie == null) {
            throw new BusinessException("影片不存在");
        }
        return movie;
    }

    /**
     * 添加影片
     * 设置默认状态为即将上映(2)，默认排序权重为0
     *
     * @param movie 影片信息
     * @return 添加后的影片
     */
    @Override
    public Movie add(Movie movie) {
        // 设置默认值
        if (movie.getStatus() == null) {
            movie.setStatus(2);  // 默认设置为即将上映状态
        }
        if (movie.getIsHot() == null) {
            movie.setIsHot(0);   // 默认不热门
        }
        if (movie.getSortOrder() == null) {
            // 新添加的影片排序权重设为当前最大权重+1，确保出现在列表最前面
            // 使用分页查询获取排序最高的影片，避免使用数据库方言 LIMIT 1
            Page<Movie> page = new Page<>(1, 1);
            LambdaQueryWrapper<Movie> qw = new LambdaQueryWrapper<Movie>()
                .orderByDesc(Movie::getSortOrder);
            Page<Movie> result = movieMapper.selectPage(page, qw);
            int maxSortOrder = 0;
            if (result.getRecords() != null && !result.getRecords().isEmpty()) {
                Movie maxSort = result.getRecords().get(0);
                maxSortOrder = (maxSort.getSortOrder() != null) ? maxSort.getSortOrder() : 0;
            }
            movie.setSortOrder(maxSortOrder + 1);
        }

        movieMapper.insert(movie);
        log.info("影片添加成功: id={}, 片名={}", movie.getId(), movie.getMovieName());
        return movie;
    }

    /**
     * 更新影片信息
     * 先检查影片是否存在，再执行更新
     *
     * @param movie 影片信息（含ID）
     * @return 更新后的影片
     */
    @Override
    public Movie update(Movie movie) {
        Movie existing = movieMapper.selectById(movie.getId());
        if (existing == null) {
            throw new BusinessException("影片不存在");
        }
        movieMapper.updateById(movie);
        log.info("影片更新成功: id={}, 片名={}", movie.getId(), movie.getMovieName());
        return movieMapper.selectById(movie.getId());
    }

    /**
     * 删除影片（逻辑删除）
     * MyBatis-Plus @TableLogic 自动处理逻辑删除
     *
     * @param id 影片ID
     */
    @Override
    public void delete(Long id) {
        Movie movie = movieMapper.selectById(id);
        if (movie == null) {
            throw new BusinessException("影片不存在");
        }
        movieMapper.deleteById(id);
        log.info("影片删除成功: id={}, 片名={}", id, movie.getMovieName());
    }

    /**
     * 搜索影片
     * 支持按片名、导演、主演模糊匹配
     *
     * @param keyword 搜索关键词
     * @return 匹配的影片列表
     */
    @Override
    public List<Movie> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return movieMapper.searchMovies(keyword.trim());
    }

    /**
     * 设置影片热门状态
     *
     * @param id   影片ID
     * @param isHot 是否热门（0-否 1-是）
     */
    @Override
    public void setHot(Long id, Integer isHot) {
        Movie movie = movieMapper.selectById(id);
        if (movie == null) {
            throw new BusinessException("影片不存在");
        }
        movie.setIsHot(isHot);
        movieMapper.updateById(movie);
        log.info("影片热门状态更新: id={}, isHot={}, 片名={}", id, isHot, movie.getMovieName());
    }

    /**
     * 设置影片状态
     *
     * @param id     影片ID
     * @param status 状态值（0-下架 1-上架 2-即将上映）
     */
    @Override
    public void setStatus(Long id, Integer status) {
        Movie movie = movieMapper.selectById(id);
        if (movie == null) {
            throw new BusinessException("影片不存在");
        }
        if (status < 0 || status > 2) {
            throw new BusinessException("状态值无效，有效值为: 0-下架, 1-上架, 2-即将上映");
        }
        movie.setStatus(status);
        movieMapper.updateById(movie);
        log.info("影片状态更新: id={}, status={}, 片名={}", id, status, movie.getMovieName());
    }

    /**
     * 查询热门影片
     *
     * @return 热门影片列表
     */
    @Override
    public List<Movie> getHotMovies() {
        return movieMapper.selectHotMovies();
    }
}
