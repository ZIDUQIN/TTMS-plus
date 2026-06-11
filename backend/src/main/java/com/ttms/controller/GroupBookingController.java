package com.ttms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.dto.ApiResponse;
import com.ttms.entity.GroupBooking;
import com.ttms.mapper.GroupBookingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/** B17: 团体/包场预约 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GroupBookingController {
    private final GroupBookingMapper groupBookingMapper;

    /** 用户提交团体预约 */
    @PostMapping("/user/group-bookings")
    public ApiResponse<GroupBooking> create(@RequestBody GroupBooking booking) {
        Long userId = getCurrentUserId();
        booking.setUserId(userId);
        booking.setStatus(0);
        groupBookingMapper.insert(booking);
        log.info("团体预约: userId={}, company={}, attendees={}", userId, booking.getCompanyName(), booking.getAttendeeCount());
        return ApiResponse.success("预约已提交，请等待审核", booking);
    }

    /** 用户查看预约 */
    @GetMapping("/user/group-bookings")
    public ApiResponse<Page<GroupBooking>> listMy(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Page<GroupBooking> p = new Page<>(page, size);
        LambdaQueryWrapper<GroupBooking> w = new LambdaQueryWrapper<>();
        w.eq(GroupBooking::getUserId, userId).orderByDesc(GroupBooking::getCreateTime);
        return ApiResponse.success(groupBookingMapper.selectPage(p, w));
    }

    /** 管理端: 预约列表 */
    @GetMapping("/admin/group-bookings")
    public ApiResponse<Page<GroupBooking>> adminList(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) Integer status) {
        Page<GroupBooking> p = new Page<>(page, size);
        LambdaQueryWrapper<GroupBooking> w = new LambdaQueryWrapper<>();
        if (status != null) w.eq(GroupBooking::getStatus, status);
        w.orderByDesc(GroupBooking::getCreateTime);
        return ApiResponse.success(groupBookingMapper.selectPage(p, w));
    }

    /** 管理端: 审核预约 */
    @PutMapping("/admin/group-bookings/{id}/review")
    public ApiResponse<String> review(@PathVariable Long id, @RequestBody GroupBooking review) {
        GroupBooking booking = groupBookingMapper.selectById(id);
        if (booking == null) return ApiResponse.error("预约不存在");
        booking.setStatus(review.getStatus());
        booking.setReviewNotes(review.getReviewNotes());
        booking.setReviewTime(LocalDateTime.now());
        booking.setReviewerId(getCurrentUserId());
        groupBookingMapper.updateById(booking);
        log.info("团体预约审核: id={}, status={}", id, review.getStatus());
        return ApiResponse.success("审核完成");
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
