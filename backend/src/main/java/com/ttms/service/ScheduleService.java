package com.ttms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.entity.Schedule;

import java.util.List;
import java.util.Map;

/**
 * 场次服务接口
 * 负责场次的增删改查、座位管理、时间冲突检查等业务
 */
public interface ScheduleService {

    /**
     * 分页查询场次列表
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果（含关联影片和影厅信息）
     */
    Page<Schedule> list(int page, int size);

    /**
     * 查询场次详情
     *
     * @param id 场次ID
     * @return 场次实体（含关联信息）
     */
    Schedule detail(Long id);

    /**
     * 添加场次
     * 自动检查影厅时间冲突，计算结束时间
     *
     * @param schedule 场次信息
     * @return 添加后的场次
     */
    Schedule add(Schedule schedule);

    /**
     * 更新场次
     * 检查时间冲突
     *
     * @param schedule 场次信息（含ID）
     * @return 更新后的场次
     */
    Schedule update(Schedule schedule);

    /**
     * 删除场次（逻辑删除）
     *
     * @param id 场次ID
     */
    void delete(Long id);

    /**
     * 根据影片ID查询场次
     *
     * @param movieId 影片ID
     * @return 该影片的所有场次
     */
    List<Schedule> queryByMovie(Long movieId);

    /**
     * 查询即将上映的场次
     *
     * @return 开始时间在未来的场次列表
     */
    List<Schedule> queryUpcoming();

    /**
     * 获取场次的座位矩阵
     * 如果座位尚未生成，则根据影厅的行列数自动生成
     *
     * @param scheduleId 场次ID
     * @return 包含座位矩阵的Map结构
     *         key: "schedule" - 场次信息
     *         key: "seats"   - 座位二维列表
     */
    Map<String, Object> getSeats(Long scheduleId);
}
