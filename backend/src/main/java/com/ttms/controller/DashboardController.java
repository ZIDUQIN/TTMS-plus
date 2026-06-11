package com.ttms.controller;

import com.ttms.dto.ApiResponse;
import com.ttms.service.impl.DashboardServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** B25: 数据看板 */
@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardServiceImpl dashboardService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getDashboard() {
        return ApiResponse.success(dashboardService.getDashboardData());
    }
}
