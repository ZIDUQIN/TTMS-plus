package com.ttms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.entity.Hall;
import com.ttms.entity.Movie;
import com.ttms.entity.Schedule;
import com.ttms.entity.Seat;
import com.ttms.exception.BusinessException;
import com.ttms.mapper.HallMapper;
import com.ttms.mapper.MovieMapper;
import com.ttms.mapper.ScheduleMapper;
import com.ttms.mapper.SeatMapper;
import com.ttms.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 场次服务实现类
 * 负责场次的增删改查、时间冲突检查、座位生成等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleMapper scheduleMapper;
    private final HallMapper hallMapper;
    private final MovieMapper movieMapper;
    private final SeatMapper seatMapper;

    /** 行号字母映射: 0->A, 1->B, 2->C, ..., 25->Z */
    private static final char[] ROW_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    /**
     * 分页查询场次列表
     * 联表查询影片名和影厅名
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    @Override
    public Page<Schedule> list(int page, int size) {
        Page<Schedule> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Schedule::getStartTime);

        Page<Schedule> result = scheduleMapper.selectPage(pageParam, wrapper);
        // 补充关联信息（影片名、影厅名等）
        fillScheduleInfo(result.getRecords());
        return result;
    }

    /**
     * 查询场次详情
     *
     * @param id 场次ID
     * @return 场次实体（含关联信息）
     */
    @Override
    public Schedule detail(Long id) {
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException("场次不存在");
        }
        fillScheduleInfo(Collections.singletonList(schedule));
        return schedule;
    }

    /**
     * 添加场次
     * 1. 验证影片和影厅存在
     * 2. 检查影厅该时间段是否已有其他场次（时间冲突检查）
     * 3. 根据影片时长计算结束时间
     * 4. 设置默认票价（使用影片基础票价）
     *
     * @param schedule 场次信息（需包含movieId、hallId、startTime）
     * @return 添加后的场次
     */
    @Override
    @Transactional
    public Schedule add(Schedule schedule) {
        // 验证影片存在
        Movie movie = movieMapper.selectById(schedule.getMovieId());
        if (movie == null) {
            throw new BusinessException("影片不存在");
        }
        if (movie.getStatus() == null || movie.getStatus() == 0) {
            throw new BusinessException("该影片已下架，无法排片");
        }

        // 验证影厅存在且正常
        Hall hall = hallMapper.selectById(schedule.getHallId());
        if (hall == null) {
            throw new BusinessException("影厅不存在");
        }
        if (hall.getStatus() != null && hall.getStatus() == 0) {
            throw new BusinessException("该影厅正在维护中，无法排片");
        }

        // 计算结束时间: 开始时间 + 影片时长
        if (schedule.getStartTime() == null) {
            throw new BusinessException("场次开始时间不能为空");
        }
        schedule.setEndTime(schedule.getStartTime().plusMinutes(movie.getDuration()));

        // 检查时间冲突：同一影厅内，新场次的时间不能与已有场次重叠
        checkTimeConflict(schedule.getHallId(), schedule.getStartTime(), schedule.getEndTime(), null);

        // 设置默认值
        if (schedule.getPrice() == null) {
            schedule.setPrice(movie.getBasePrice()); // 默认票价使用影片基础票价
        }
        if (schedule.getStatus() == null) {
            schedule.setStatus(1);  // 默认状态为正常放映
        }
        if (schedule.getSoldCount() == null) {
            schedule.setSoldCount(0); // 初始已售座位数为0
        }

        scheduleMapper.insert(schedule);
        log.info("场次添加成功: id={}, 影片={}, 影厅={}, 开始时间={}",
            schedule.getId(), movie.getMovieName(), hall.getHallName(), schedule.getStartTime());

        fillScheduleInfo(Collections.singletonList(schedule));
        return schedule;
    }

    /**
     * 更新场次
     * 同样进行时间冲突检查
     *
     * @param schedule 场次信息（含ID）
     * @return 更新后的场次
     */
    @Override
    @Transactional
    public Schedule update(Schedule schedule) {
        Schedule existing = scheduleMapper.selectById(schedule.getId());
        if (existing == null) {
            throw new BusinessException("场次不存在");
        }

        // 如果修改了影片ID，重新获取影片信息
        Movie movie;
        if (schedule.getMovieId() != null && !schedule.getMovieId().equals(existing.getMovieId())) {
            movie = movieMapper.selectById(schedule.getMovieId());
            if (movie == null) {
                throw new BusinessException("影片不存在");
            }
        } else {
            movie = movieMapper.selectById(existing.getMovieId());
        }

        // 重新计算结束时间
        LocalDateTime newStartTime = schedule.getStartTime() != null ? schedule.getStartTime() : existing.getStartTime();
        if (movie != null) {
            schedule.setEndTime(newStartTime.plusMinutes(movie.getDuration()));
        }

        // 检查时间冲突（排除自身）
        Long hallId = schedule.getHallId() != null ? schedule.getHallId() : existing.getHallId();
        checkTimeConflict(hallId, newStartTime, schedule.getEndTime(), existing.getId());

        scheduleMapper.updateById(schedule);
        log.info("场次更新成功: id={}", schedule.getId());

        Schedule updated = scheduleMapper.selectById(schedule.getId());
        fillScheduleInfo(Collections.singletonList(updated));
        return updated;
    }

    /**
     * 删除场次（逻辑删除）
     * 删除前检查是否有已售出的座位
     *
     * @param id 场次ID
     */
    @Override
    public void delete(Long id) {
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException("场次不存在");
        }
        // 如果已售出座位，不允许删除
        if (schedule.getSoldCount() != null && schedule.getSoldCount() > 0) {
            throw new BusinessException("该场次已售出" + schedule.getSoldCount() + "张票，无法删除");
        }
        scheduleMapper.deleteById(id);
        log.info("场次删除成功: id={}", id);
    }

    /**
     * 根据影片ID查询场次列表
     *
     * @param movieId 影片ID
     * @return 该影片的所有场次（含关联信息）
     */
    @Override
    public List<Schedule> queryByMovie(Long movieId) {
        List<Schedule> schedules = scheduleMapper.selectByMovieId(movieId);
        if (schedules != null && !schedules.isEmpty()) {
            fillScheduleInfo(schedules);
        }
        return schedules;
    }

    /**
     * 查询即将上映的场次
     *
     * @return 开始时间在未来的场次列表（含关联信息）
     */
    @Override
    public List<Schedule> queryUpcoming() {
        List<Schedule> schedules = scheduleMapper.selectUpcoming();
        if (schedules != null && !schedules.isEmpty()) {
            fillScheduleInfo(schedules);
        }
        return schedules;
    }

    /**
     * 获取场次的座位矩阵
     * 如果座位尚未生成，则根据影厅的行列数自动创建座位
     * 返回的数据结构中包含场次信息和座位二维列表
     *
     * @param scheduleId 场次ID
     * @return Map: "schedule" -> 场次信息, "seats" -> 座位二维列表
     */
    @Override
    @Transactional
    public Map<String, Object> getSeats(Long scheduleId) {
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("场次不存在");
        }
        fillScheduleInfo(Collections.singletonList(schedule));

        // 查询该场次的座位
        List<Seat> seatList = seatMapper.selectByScheduleId(scheduleId);

        // 如果座位数据为空，则根据影厅布局自动生成
        if (seatList == null || seatList.isEmpty()) {
            Hall hall = hallMapper.selectById(schedule.getHallId());
            if (hall == null) {
                throw new BusinessException("影厅信息不存在");
            }
            seatList = generateSeats(scheduleId, hall);
        }

        // 构建二维座位矩阵（按行号、列号组织）
        Map<Integer, List<Seat>> rowMap = new LinkedHashMap<>();
        for (Seat seat : seatList) {
            rowMap.computeIfAbsent(seat.getSeatRow(), k -> new ArrayList<>()).add(seat);
        }

        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("schedule", schedule);
        result.put("seats", rowMap.values().stream().collect(Collectors.toList()));
        result.put("rowCount", schedule.getHallRowCount());
        result.put("colCount", schedule.getHallColCount());

        return result;
    }

    /**
     * 检查同一影厅内是否存在时间冲突的场次
     * 冲突判断逻辑：两个场次的时间段不能有重叠
     * 新开始 < 旧结束 AND 新结束 > 旧开始  即视为时间冲突
     *
     * @param hallId    影厅ID
     * @param startTime 新场次开始时间
     * @param endTime   新场次结束时间
     * @param excludeId 需要排除的场次ID（更新时排除自身）
     */
    private void checkTimeConflict(Long hallId, LocalDateTime startTime, LocalDateTime endTime, Long excludeId) {
        if (startTime == null || endTime == null) {
            return;
        }

        List<Schedule> hallSchedules = scheduleMapper.selectByHallId(hallId);
        if (hallSchedules == null || hallSchedules.isEmpty()) {
            return;
        }

        for (Schedule existing : hallSchedules) {
            // 跳过自身（更新场景）
            if (excludeId != null && excludeId.equals(existing.getId())) {
                continue;
            }
            // 只检查正常状态（status=1）的场次
            if (existing.getStatus() != null && existing.getStatus() != 1) {
                continue;
            }
            // 跳过已结束的场次
            if (existing.getEndTime() != null && existing.getEndTime().isBefore(LocalDateTime.now())) {
                continue;
            }
            // 时间冲突检测：两个时间段有交集
            if (existing.getStartTime() != null && existing.getEndTime() != null) {
                if (startTime.isBefore(existing.getEndTime()) && endTime.isAfter(existing.getStartTime())) {
                    throw new BusinessException("该时间段影厅已被占用！已有场次: "
                        + existing.getMovieName() + "，时间段: "
                        + existing.getStartTime() + " ~ " + existing.getEndTime()
                        + "，请选择其他时间或影厅");
                }
            }
        }
    }

    /**
     * 根据影厅布局自动生成座位
     * 行号使用字母表示（A, B, C, ...）
     * 列号使用数字表示（1, 2, 3, ...）
     * 座位编号格式: "A-05"
     *
     * @param scheduleId 场次ID
     * @param hall       影厅信息（含行列数）
     * @return 生成的座位列表
     */
    private List<Seat> generateSeats(Long scheduleId, Hall hall) {
        // 二次检查：防止并发请求重复生成座位
        List<Seat> existingSeats = seatMapper.selectByScheduleId(scheduleId);
        if (existingSeats != null && !existingSeats.isEmpty()) {
            log.debug("座位已存在，跳过生成: scheduleId={}, 座位数={}", scheduleId, existingSeats.size());
            return existingSeats;
        }

        List<Seat> seats = new ArrayList<>();
        int rowCount = hall.getRowCount() != null ? hall.getRowCount() : 10;
        int colCount = hall.getColCount() != null ? hall.getColCount() : 15;

        // 解析影厅座位布局，获取不可用的座位位置
        Set<String> disabledSeats = parseDisabledSeats(hall.getSeatLayout());

        for (int row = 1; row <= rowCount; row++) {
            String rowLetter = getRowLetter(row - 1);
            for (int col = 1; col <= colCount; col++) {
                String seatKey = row + "-" + col;

                Seat seat = new Seat();
                seat.setScheduleId(scheduleId);
                seat.setSeatRow(row);
                seat.setSeatCol(col);
                seat.setSeatNumber(rowLetter + "-" + String.format("%02d", col));

                // 如果该座位在布局中被标记为不可用，设置为过道/不可用状态
                if (disabledSeats.contains(seatKey)) {
                    seat.setStatus(3); // 3 = 过道/不可用
                } else {
                    seat.setStatus(0); // 0 = 空闲
                }
                seatMapper.insert(seat);
                seats.add(seat);
            }
        }
        long usableCount = seats.stream().filter(s -> s.getStatus() != 3).count();
        log.info("场次座位自动生成完成: scheduleId={}, 总座位数={}, 可用座位={} ({}x{})",
            scheduleId, seats.size(), usableCount, rowCount, colCount);
        return seats;
    }

    /**
     * 统计不可用座位数量
     * @param seatLayout 座位布局JSON，格式: ["1-5","2-10","3-8"]
     * @return 不可用座位数
     */
    private int countDisabledSeats(String seatLayout) {
        if (seatLayout == null || seatLayout.isBlank()) {
            return 0;
        }
        try {
            String content = seatLayout.trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                content = content.substring(1, content.length() - 1);
                if (content.isBlank()) return 0;
                return content.split(",").length;
            }
        } catch (Exception e) {
            log.warn("统计不可用座位数失败: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * 解析座位布局JSON，提取不可用座位位置集合
     * JSON格式: ["1-5","2-10","3-8"]
     */
    private Set<String> parseDisabledSeats(String seatLayout) {
        Set<String> disabled = new java.util.HashSet<>();
        if (seatLayout == null || seatLayout.isBlank()) {
            return disabled;
        }
        try {
            // 简单解析JSON数组: ["1-5","2-10"]
            String content = seatLayout.trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                content = content.substring(1, content.length() - 1);
                for (String part : content.split(",")) {
                    String seatKey = part.trim().replaceAll("\"", "");
                    if (!seatKey.isEmpty()) {
                        disabled.add(seatKey);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析座位布局失败: {}", e.getMessage());
        }
        return disabled;
    }

    /**
     * 将行索引转换为字母表示
     * 0->A, 1->B, ..., 25->Z, 26->AA, 27->AB, ...
     *
     * @param rowIndex 行索引（从0开始）
     * @return 行字母表示
     */
    private String getRowLetter(int rowIndex) {
        StringBuilder sb = new StringBuilder();
        int index = rowIndex;
        while (index >= 0) {
            sb.insert(0, ROW_LETTERS[index % 26]);
            index = index / 26 - 1;
        }
        return sb.toString();
    }

    /**
     * 为场次列表补充影片名、影厅名等关联信息
     * 这些字段在Schedule实体中标记为 @TableField(exist = false)
     * MyBatis-Plus不会自动查询
     *
     * @param schedules 场次列表
     */
    private void fillScheduleInfo(List<Schedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return;
        }
        // 批量查询关联影片，避免N+1查询
        Set<Long> movieIds = schedules.stream()
                .map(Schedule::getMovieId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Movie> movieMap = new HashMap<>();
        if (!movieIds.isEmpty()) {
            movieMapper.selectBatchIds(movieIds)
                    .forEach(m -> movieMap.put(m.getId(), m));
        }
        // 批量查询关联影厅，避免N+1查询
        Set<Long> hallIds = schedules.stream()
                .map(Schedule::getHallId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Hall> hallMap = new HashMap<>();
        if (!hallIds.isEmpty()) {
            hallMapper.selectBatchIds(hallIds)
                    .forEach(h -> hallMap.put(h.getId(), h));
        }
        // 补充关联信息
        for (Schedule schedule : schedules) {
            if (schedule.getMovieId() != null) {
                Movie movie = movieMap.get(schedule.getMovieId());
                if (movie != null) {
                    schedule.setMovieName(movie.getMovieName());
                    schedule.setDuration(movie.getDuration());
                    schedule.setPosterUrl(movie.getPosterUrl());
                }
            }
            if (schedule.getHallId() != null) {
                Hall hall = hallMap.get(schedule.getHallId());
                if (hall != null) {
                    schedule.setHallName(hall.getHallName());
                    schedule.setHallRowCount(hall.getRowCount());
                    schedule.setHallColCount(hall.getColCount());
                    // 计算可用座位数：总座位 - 已售 - 已锁定 - 不可用座位
                    int totalSeats = (hall.getRowCount() != null ? hall.getRowCount() : 0)
                                   * (hall.getColCount() != null ? hall.getColCount() : 0);
                    int disabledCount = countDisabledSeats(hall.getSeatLayout());
                    int soldCount = schedule.getSoldCount() != null ? schedule.getSoldCount() : 0;
                    // 同时查询已锁定（status=1）但未支付的座位数
                    int lockedCount = seatMapper.selectCount(
                        new LambdaQueryWrapper<Seat>()
                            .eq(Seat::getScheduleId, schedule.getId())
                            .eq(Seat::getStatus, 1)
                    ).intValue();
                    schedule.setAvailableSeats(Math.max(0, totalSeats - disabledCount - soldCount - lockedCount));
                }
            }
        }
    }
}
