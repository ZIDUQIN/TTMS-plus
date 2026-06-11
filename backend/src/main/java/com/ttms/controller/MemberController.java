package com.ttms.controller;

import com.ttms.dto.ApiResponse;
import com.ttms.entity.MemberLevel;
import com.ttms.entity.User;
import com.ttms.exception.BusinessException;
import com.ttms.mapper.MemberLevelMapper;
import com.ttms.mapper.UserMapper;
import com.ttms.service.impl.MemberServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** B15: 会员体系 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class MemberController {
    private final MemberServiceImpl memberService;
    private final MemberLevelMapper memberLevelMapper;
    private final UserMapper userMapper;

    // ===== 会员等级 CRUD（管理端）=====

    /** 获取所有会员等级 */
    @GetMapping("/api/admin/member-levels")
    public ApiResponse<List<MemberLevel>> listLevels() {
        return ApiResponse.success(memberService.listLevels());
    }

    /** 管理端: 新增会员等级 */
    @PostMapping("/api/admin/member-levels")
    public ApiResponse<MemberLevel> saveLevel(@RequestBody MemberLevel level) {
        memberLevelMapper.insert(level);
        return ApiResponse.success("会员等级已创建", level);
    }

    /** 管理端: 更新会员等级 */
    @PutMapping("/api/admin/member-levels")
    public ApiResponse<MemberLevel> updateLevel(@RequestBody MemberLevel level) {
        memberLevelMapper.updateById(level);
        return ApiResponse.success("会员等级已更新", level);
    }

    /** 管理端: 删除会员等级 */
    @DeleteMapping("/api/admin/member-levels/{id}")
    public ApiResponse<String> deleteLevel(@PathVariable Long id) {
        memberLevelMapper.deleteById(id);
        return ApiResponse.success("会员等级已删除");
    }

    // ===== 会员用户管理（管理端）=====

    /** 管理端: 查看所有用户（含会员等级/积分/余额） */
    @GetMapping("/api/admin/members")
    public ApiResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Map<String, Object>>>
        listMembers(@RequestParam(defaultValue = "1") int page,
                    @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(memberService.listMemberUsers(page, size));
    }

    /** 管理端: 手动设置用户会员等级 */
    @PutMapping("/api/admin/members/{userId}/level")
    public ApiResponse<String> setMemberLevel(@PathVariable Long userId,
                                               @RequestBody Map<String, Object> body) {
        Long levelId = body.get("levelId") != null ? Long.valueOf(body.get("levelId").toString()) : null;
        memberService.setUserLevel(userId, levelId);
        return ApiResponse.success(levelId != null ? "会员等级已设置" : "已取消会员等级");
    }

    /** 管理端: 手动调整用户积分（正数增加、负数扣减） */
    @PutMapping("/api/admin/members/{userId}/points")
    public ApiResponse<String> adjustPoints(@PathVariable Long userId,
                                             @RequestBody Map<String, Object> body) {
        int delta = Integer.parseInt(body.get("delta").toString());
        memberService.adjustPoints(userId, delta);
        return ApiResponse.success("积分已调整");
    }

    /** 管理端: 删除用户（原始SQL软删除，绕过一切ORM干扰） */
    @DeleteMapping("/api/admin/members/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        if (userId.equals(currentUserId)) {
            return ApiResponse.error("不能删除自己的账户");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        int rows = userMapper.softDeleteById(userId);
        if (rows == 0) {
            return ApiResponse.error("销户失败，该用户可能已被销户");
        }
        log.info("管理员销户用户: operatorId={}, targetUserId={}, username={}",
            currentUserId, userId, user.getUsername());
        return ApiResponse.success("用户「" + user.getUsername() + "」已销户");
    }

    // ===== 用户端会员信息 =====

    /** 用户端: 我的会员信息（完整版：等级/积分/余额/下一级进度） */
    @GetMapping("/api/user/membership")
    public ApiResponse<Map<String, Object>> myMembership() {
        Long userId = getCurrentUserId();
        return ApiResponse.success(memberService.getUserMembershipInfo(userId));
    }

    /** 积分兑换优惠券 */
    @PostMapping("/api/user/redeem-points")
    public ApiResponse<Map<String, Object>> redeemPoints(@RequestBody Map<String, Object> params) {
        Long userId = getCurrentUserId();
        int points = Integer.parseInt(params.get("points").toString());
        return ApiResponse.success(memberService.redeemPoints(userId, points));
    }

    /** 储值充值 */
    @PostMapping("/api/user/recharge")
    public ApiResponse<String> recharge(@RequestBody Map<String, Object> params) {
        Long userId = getCurrentUserId();
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        memberService.recharge(userId, amount);
        return ApiResponse.success("充值成功");
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
