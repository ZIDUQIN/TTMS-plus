package com.ttms.service.impl;

import com.ttms.entity.Employee;
import com.ttms.entity.Shift;
import com.ttms.entity.ShiftRecord;
import com.ttms.exception.BusinessException;
import com.ttms.mapper.EmployeeMapper;
import com.ttms.mapper.ShiftMapper;
import com.ttms.mapper.ShiftRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * B22: 交接班服务
 * 管理员工上班签到、下班交班结算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftServiceImpl {

    private final ShiftMapper shiftMapper;
    private final ShiftRecordMapper shiftRecordMapper;
    private final EmployeeMapper employeeMapper;

    /** 开始上班 */
    @Transactional
    public Shift startShift(Long employeeId) {
        Shift active = shiftMapper.findActiveShift(employeeId);
        if (active != null) {
            throw new BusinessException("您已有进行中的班次，请先结束当前班次");
        }
        Shift shift = new Shift();
        shift.setEmployeeId(employeeId);
        shift.setStartTime(LocalDateTime.now());
        shift.setStatus(0);
        shiftMapper.insert(shift);
        log.info("上班签到: employeeId={}, shiftId={}", employeeId, shift.getId());
        return shift;
    }

    /** 下班交班 */
    @Transactional
    public Shift endShift(Long employeeId, ShiftRecord record) {
        Shift shift = shiftMapper.findActiveShift(employeeId);
        if (shift == null) {
            throw new BusinessException("未找到进行中的班次");
        }
        shift.setEndTime(LocalDateTime.now());
        shift.setStatus(1);
        shiftMapper.updateById(shift);

        record.setShiftId(shift.getId());
        record.setCreateTime(LocalDateTime.now());
        shiftRecordMapper.insert(record);
        log.info("下班交班: employeeId={}, shiftId={}, cash={}, wx={}, ali={}",
            employeeId, shift.getId(), record.getCashCollected(), record.getWechatCollected(), record.getAlipayCollected());
        return shift;
    }

    /** 查询班次列表 */
    public List<Shift> listShifts(int page, int size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Shift> pageParam =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Shift> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.orderByDesc(Shift::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Shift> result = shiftMapper.selectPage(pageParam, wrapper);
        // 补充员工姓名
        for (Shift s : result.getRecords()) {
            Employee e = employeeMapper.selectById(s.getEmployeeId());
            if (e != null) s.setEmployeeName(e.getRealName());
        }
        return result.getRecords();
    }

    /** 查询当前进行中的班次 */
    public Shift getActiveShift(Long employeeId) {
        return shiftMapper.findActiveShift(employeeId);
    }
}
