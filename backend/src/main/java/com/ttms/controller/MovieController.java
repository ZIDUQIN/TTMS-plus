package com.ttms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.dto.ApiResponse;
import com.ttms.entity.Movie;
import com.ttms.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 影片控制器
 * 处理影片相关的所有HTTP请求
 * 部分接口对外公开（列表、详情、搜索），部分接口需要管理员权限
 */
@Slf4j
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    /**
     * 查询影片列表（公开接口）
     * GET /api/movies/list?page=1&size=10&status=1
     *
     * @param page   页码（默认1）
     * @param size   每页大小（默认10）
     * @param status 状态筛选（可选：0-下架 1-上架 2-即将上映）
     * @return 分页影片列表
     */
    @GetMapping("/list")
    public ApiResponse<Page<Movie>> list(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "100") int size,
                                          @RequestParam(required = false) Integer status) {
        log.debug("查询影片列表: page={}, size={}, status={}", page, size, status);
        Page<Movie> result = movieService.list(page, size, status);
        return ApiResponse.success(result);
    }

    /**
     * 查询影片详情（公开接口）
     * GET /api/movies/detail/{id}
     *
     * @param id 影片ID
     * @return 影片详情
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<Movie> detail(@PathVariable Long id) {
        log.debug("查询影片详情: id={}", id);
        Movie movie = movieService.detail(id);
        return ApiResponse.success(movie);
    }

    /**
     * 查询热门影片（公开接口）
     * GET /api/movies/hot
     *
     * @return 热门影片列表
     */
    @GetMapping("/hot")
    public ApiResponse<List<Movie>> hot() {
        log.debug("查询热门影片");
        List<Movie> hotMovies = movieService.getHotMovies();
        return ApiResponse.success(hotMovies);
    }

    /**
     * 搜索影片（公开接口）
     * GET /api/movies/search?keyword=xxx
     *
     * @param keyword 搜索关键词
     * @return 匹配的影片列表
     */
    @GetMapping("/search")
    public ApiResponse<List<Movie>> search(@RequestParam String keyword) {
        log.debug("搜索影片: keyword={}", keyword);
        List<Movie> movies = movieService.search(keyword);
        return ApiResponse.success(movies);
    }

    /**
     * 添加影片（管理端接口）
     * POST /api/movies/add
     *
     * @param movie 影片信息
     * @return 添加后的影片
     */
    @PostMapping("/add")
    public ApiResponse<Movie> add(@RequestBody Movie movie) {
        log.info("添加影片: movieName={}", movie.getMovieName());
        Movie result = movieService.add(movie);
        return ApiResponse.success("影片添加成功", result);
    }

    /**
     * 更新影片（管理端接口）
     * PUT /api/movies/update
     *
     * @param movie 影片信息（含ID）
     * @return 更新后的影片
     */
    @PutMapping("/update")
    public ApiResponse<Movie> update(@RequestBody Movie movie) {
        log.info("更新影片: id={}, movieName={}", movie.getId(), movie.getMovieName());
        Movie result = movieService.update(movie);
        return ApiResponse.success("影片更新成功", result);
    }

    /**
     * 删除影片（管理端接口）
     * DELETE /api/movies/delete/{id}
     *
     * @param id 影片ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("删除影片: id={}", id);
        movieService.delete(id);
        return ApiResponse.success("影片删除成功");
    }

    /**
     * 设置影片热门状态（管理端接口）
     * PUT /api/movies/set-hot
     *
     * @param params 包含id和isHot的Map
     * @return 操作结果
     */
    @PutMapping("/set-hot")
    public ApiResponse<Void> setHot(@RequestBody Map<String, Object> params) {
        Object idObj = params.get("id");
        Object isHotObj = params.get("isHot");
        if (idObj == null || isHotObj == null) {
            return ApiResponse.badRequest("参数id和isHot不能为空");
        }
        Long id = Long.valueOf(idObj.toString());
        Integer isHot = Integer.valueOf(isHotObj.toString());
        log.info("设置热门: id={}, isHot={}", id, isHot);
        movieService.setHot(id, isHot);
        return ApiResponse.success("热门设置成功");
    }

    /**
     * 设置影片状态（管理端接口）
     * PUT /api/movies/set-status
     *
     * @param params 包含id和status的Map
     * @return 操作结果
     */
    @PutMapping("/set-status")
    public ApiResponse<Void> setStatus(@RequestBody Map<String, Object> params) {
        Object idObj = params.get("id");
        Object statusObj = params.get("status");
        if (idObj == null || statusObj == null) {
            return ApiResponse.badRequest("参数id和status不能为空");
        }
        Long id = Long.valueOf(idObj.toString());
        Integer status = Integer.valueOf(statusObj.toString());
        log.info("设置影片状态: id={}, status={}", id, status);
        movieService.setStatus(id, status);
        return ApiResponse.success("状态更新成功");
    }
}
