package com.ttms.controller;

import com.ttms.dto.ApiResponse;
import com.ttms.entity.Report;
import com.ttms.service.impl.ReportServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** B23: 报表管理 */
@Slf4j
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportServiceImpl reportService;

    @GetMapping
    public ApiResponse<List<Report>> list(@RequestParam(required = false) String type,
                                           @RequestParam(required = false) String startDate,
                                           @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        return ApiResponse.success(reportService.list(type, start, end));
    }

    /** 手动触发日报生成 */
    @PostMapping("/generate")
    public ApiResponse<String> generate(@RequestParam(defaultValue = "DAILY") String type,
                                         @RequestParam(required = false) String date) {
        LocalDate reportDate = date != null ? LocalDate.parse(date) : LocalDate.now().minusDays(1);
        reportService.generateReport(type, reportDate);
        return ApiResponse.success("报表已生成");
    }
}
