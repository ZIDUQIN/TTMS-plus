package com.ttms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.dto.ApiResponse;
import com.ttms.entity.Hall;
import com.ttms.service.HallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 影厅控制器
 * 处理影厅管理的所有HTTP请求
 * 所有接口均需要管理员权限（/api/admin/halls/**）
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/halls")
@RequiredArgsConstructor
public class HallController {

    private final HallService hallService;

    /**
     * 查询影厅列表
     * GET /api/admin/halls/list?page=1&size=10
     *
     * @param page 页码（默认1）
     * @param size 每页大小（默认10）
     * @return 分页影厅列表
     */
    @GetMapping("/list")
    public ApiResponse<Page<Hall>> list(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        log.debug("查询影厅列表: page={}, size={}", page, size);
        Page<Hall> result = hallService.list(page, size);
        return ApiResponse.success(result);
    }

    /**
     * 查询影厅详情
     * GET /api/admin/halls/detail/{id}
     *
     * @param id 影厅ID
     * @return 影厅详情
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<Hall> detail(@PathVariable Long id) {
        log.debug("查询影厅详情: id={}", id);
        Hall hall = hallService.detail(id);
        return ApiResponse.success(hall);
    }

    /**
     * 添加影厅
     * POST /api/admin/halls/add
     *
     * @param hall 影厅信息
     * @return 添加后的影厅
     */
    @PostMapping("/add")
    public ApiResponse<Hall> add(@RequestBody Hall hall) {
        log.info("添加影厅: hallName={}", hall.getHallName());
        Hall result = hallService.add(hall);
        return ApiResponse.success("影厅添加成功", result);
    }

    /**
     * 更新影厅
     * PUT /api/admin/halls/update
     *
     * @param hall 影厅信息（含ID）
     * @return 更新后的影厅
     */
    @PutMapping("/update")
    public ApiResponse<Hall> update(@RequestBody Hall hall) {
        log.info("更新影厅: id={}, hallName={}", hall.getId(), hall.getHallName());
        Hall result = hallService.update(hall);
        return ApiResponse.success("影厅更新成功", result);
    }

    /**
     * 删除影厅
     * DELETE /api/admin/halls/delete/{id}
     *
     * @param id 影厅ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("删除影厅: id={}", id);
        hallService.delete(id);
        return ApiResponse.success("影厅删除成功");
    }

    /**
     * 设置影厅状态
     * PUT /api/admin/halls/set-status
     *
     * @param params 包含id和status的Map
     * @return 操作结果
     */
    @PutMapping("/set-status")
    public ApiResponse<Void> setStatus(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        log.info("设置影厅状态: id={}, status={}", id, status);
        hallService.setStatus(id, status);
        return ApiResponse.success("影厅状态更新成功");
    }
}
