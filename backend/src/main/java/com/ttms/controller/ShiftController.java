package com.ttms.controller;

import com.ttms.dto.ApiResponse;
import com.ttms.entity.Shift;
import com.ttms.entity.ShiftRecord;
import com.ttms.service.impl.ShiftServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** B22: 交接班管理 */
@Slf4j
@RestController
@RequestMapping("/api/admin/shifts")
@RequiredArgsConstructor
public class ShiftController {
    private final ShiftServiceImpl shiftService;

    /** 上班签到 */
    @PostMapping("/start")
    public ApiResponse<Shift> startShift() {
        Long employeeId = getCurrentUserId();
        return ApiResponse.success("上班签到成功", shiftService.startShift(employeeId));
    }

    /** 下班交班 */
    @PostMapping("/end")
    public ApiResponse<Shift> endShift(@RequestBody ShiftRecord record) {
        Long employeeId = getCurrentUserId();
        return ApiResponse.success("交班成功", shiftService.endShift(employeeId, record));
    }

    /** 查询当前班次 */
    @GetMapping("/active")
    public ApiResponse<Shift> activeShift() {
        Long employeeId = getCurrentUserId();
        return ApiResponse.success(shiftService.getActiveShift(employeeId));
    }

    /** 班次历史列表 */
    @GetMapping("/list")
    public ApiResponse<List<Shift>> list(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(shiftService.listShifts(page, size));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
