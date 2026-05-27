package com.ttms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.entity.Hall;
import com.ttms.entity.Schedule;
import com.ttms.exception.BusinessException;
import com.ttms.mapper.HallMapper;
import com.ttms.mapper.ScheduleMapper;
import com.ttms.service.HallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 影厅服务实现类
 * 负责影厅的增删改查、状态管理等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HallServiceImpl implements HallService {

    private final HallMapper hallMapper;
    private final ScheduleMapper scheduleMapper;

    /**
     * 分页查询影厅列表
     * 按创建时间倒序排列
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    @Override
    public Page<Hall> list(int page, int size) {
        Page<Hall> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Hall> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Hall::getCreateTime);
        return hallMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 查询影厅详情
     *
     * @param id 影厅ID
     * @return 影厅实体
     */
    @Override
    public Hall detail(Long id) {
        Hall hall = hallMapper.selectById(id);
        if (hall == null) {
            throw new BusinessException("影厅不存在");
        }
        return hall;
    }

    /**
     * 添加影厅
     * 自动计算总容量 = 行数 x 列数
     *
     * @param hall 影厅信息
     * @return 添加后的影厅
     */
    @Override
    public Hall add(Hall hall) {
        // 默认状态为正常
        if (hall.getStatus() == null) {
            hall.setStatus(1);
        }
        // 自动计算容量
        if (hall.getRowCount() != null && hall.getColCount() != null) {
            hall.setCapacity(hall.getRowCount() * hall.getColCount());
        }
        hallMapper.insert(hall);
        log.info("影厅添加成功: id={}, 名称={}, 容量={}", hall.getId(), hall.getHallName(), hall.getCapacity());
        return hall;
    }

    /**
     * 更新影厅信息
     * 重新计算总容量
     *
     * @param hall 影厅信息（含ID）
     * @return 更新后的影厅
     */
    @Override
    public Hall update(Hall hall) {
        Hall existing = hallMapper.selectById(hall.getId());
        if (existing == null) {
            throw new BusinessException("影厅不存在");
        }
        // 重新计算容量
        if (hall.getRowCount() != null && hall.getColCount() != null) {
            hall.setCapacity(hall.getRowCount() * hall.getColCount());
        }
        hallMapper.updateById(hall);
        log.info("影厅更新成功: id={}, 名称={}", hall.getId(), hall.getHallName());
        return hallMapper.selectById(hall.getId());
    }

    /**
     * 删除影厅（逻辑删除）
     * 删除前检查是否有未开始或进行中的场次
     *
     * @param id 影厅ID
     */
    @Override
    public void delete(Long id) {
        Hall hall = hallMapper.selectById(id);
        if (hall == null) {
            throw new BusinessException("影厅不存在");
        }

        // 检查该影厅是否有未结束的场次（结束时间在当前时间之后的场次）
        List<Schedule> activeSchedules = scheduleMapper.selectByHallId(id);
        if (activeSchedules != null && !activeSchedules.isEmpty()) {
            boolean hasActive = activeSchedules.stream()
                .anyMatch(s -> s.getEndTime() != null && s.getEndTime().isAfter(LocalDateTime.now())
                       && s.getStatus() == 1);
            if (hasActive) {
                throw new BusinessException("该影厅存在进行中的场次，无法删除");
            }
        }

        hallMapper.deleteById(id);
        log.info("影厅删除成功: id={}, 名称={}", id, hall.getHallName());
    }

    /**
     * 设置影厅状态
     *
     * @param id     影厅ID
     * @param status 状态值（0-维护中 1-正常）
     */
    @Override
    public void setStatus(Long id, Integer status) {
        Hall hall = hallMapper.selectById(id);
        if (hall == null) {
            throw new BusinessException("影厅不存在");
        }
        if (status != 0 && status != 1) {
            throw new BusinessException("状态值无效，有效值为: 0-维护中, 1-正常");
        }
        hall.setStatus(status);
        hallMapper.updateById(hall);
        log.info("影厅状态更新: id={}, status={}, 名称={}", id, status, hall.getHallName());
    }
}
