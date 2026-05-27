package com.ttms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.dto.ApiResponse;
import com.ttms.entity.Schedule;
import com.ttms.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 场次控制器
 * 处理场次查询和管理的HTTP请求
 * 查询接口对外公开，管理接口需要管理员权限
 */
@Slf4j
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // ==================== 公开查询接口 ====================

    /**
     * 根据影片ID查询场次列表（公开接口）
     * GET /api/schedules/query/movie/{movieId}
     *
     * @param movieId 影片ID
     * @return 该影片的所有场次
     */
    @GetMapping("/query/movie/{movieId}")
    public ApiResponse<List<Schedule>> queryByMovie(@PathVariable Long movieId) {
        log.debug("查询影片场次: movieId={}", movieId);
        List<Schedule> schedules = scheduleService.queryByMovie(movieId);
        return ApiResponse.success(schedules);
    }

    /**
     * 查询即将上映的场次（公开接口）
     * GET /api/schedules/query/upcoming
     *
     * @return 即将上映场次列表
     */
    @GetMapping("/query/upcoming")
    public ApiResponse<List<Schedule>> queryUpcoming() {
        log.debug("查询即将上映场次");
        List<Schedule> schedules = scheduleService.queryUpcoming();
        return ApiResponse.success(schedules);
    }

    /**
     * 获取场次的座位矩阵（公开接口）
     * GET /api/schedules/query/{scheduleId}/seats
     *
     * @param scheduleId 场次ID
     * @return 座位矩阵（含场次信息和二维座位列表）
     */
    @GetMapping("/query/{scheduleId}/seats")
    public ApiResponse<Map<String, Object>> getSeats(@PathVariable Long scheduleId) {
        log.debug("查询场次座位: scheduleId={}", scheduleId);
        Map<String, Object> seats = scheduleService.getSeats(scheduleId);
        return ApiResponse.success(seats);
    }

    // ==================== 管理端接口 ====================

    /**
     * 分页查询场次列表（管理端）
     * GET /api/schedules/list?page=1&size=10
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页场次列表
     */
    @GetMapping("/list")
    public ApiResponse<Page<Schedule>> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        log.debug("查询场次列表: page={}, size={}", page, size);
        Page<Schedule> result = scheduleService.list(page, size);
        return ApiResponse.success(result);
    }

    /**
     * 查询场次详情（管理端）
     * GET /api/schedules/detail/{id}
     *
     * @param id 场次ID
     * @return 场次详情
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<Schedule> detail(@PathVariable Long id) {
        log.debug("查询场次详情: id={}", id);
        Schedule schedule = scheduleService.detail(id);
        return ApiResponse.success(schedule);
    }

    /**
     * 添加场次（管理端）
     * POST /api/schedules/add
     *
     * @param schedule 场次信息
     * @return 添加后的场次
     */
    @PostMapping("/add")
    public ApiResponse<Schedule> add(@RequestBody Schedule schedule) {
        log.info("添加场次: movieId={}, hallId={}, startTime={}",
            schedule.getMovieId(), schedule.getHallId(), schedule.getStartTime());
        Schedule result = scheduleService.add(schedule);
        return ApiResponse.success("场次添加成功", result);
    }

    /**
     * 更新场次（管理端）
     * PUT /api/schedules/update
     *
     * @param schedule 场次信息（含ID）
     * @return 更新后的场次
     */
    @PutMapping("/update")
    public ApiResponse<Schedule> update(@RequestBody Schedule schedule) {
        log.info("更新场次: id={}", schedule.getId());
        Schedule result = scheduleService.update(schedule);
        return ApiResponse.success("场次更新成功", result);
    }

    /**
     * 删除场次（管理端）
     * DELETE /api/schedules/delete/{id}
     *
     * @param id 场次ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("删除场次: id={}", id);
        scheduleService.delete(id);
        return ApiResponse.success("场次删除成功");
    }
}
