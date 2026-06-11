package com.ttms.controller;

import com.ttms.dto.ApiResponse;
import com.ttms.entity.Coupon;
import com.ttms.entity.UserCoupon;
import com.ttms.mapper.CouponMapper;
import com.ttms.service.impl.CouponServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** B16: 优惠券管理 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class CouponController {
    private final CouponServiceImpl couponService;
    private final CouponMapper couponMapper;

    /** 管理端: 优惠券列表 */
    @GetMapping("/api/admin/coupons")
    public ApiResponse<List<Coupon>> listAll() {
        return ApiResponse.success(couponService.listAvailable());
    }

    /** 管理端: 创建优惠券 */
    @PostMapping("/api/admin/coupons")
    public ApiResponse<Coupon> create(@RequestBody Coupon coupon) {
        couponMapper.insert(coupon);
        return ApiResponse.success("优惠券创建成功", coupon);
    }

    /** 管理端: 更新优惠券 */
    @PutMapping("/api/admin/coupons")
    public ApiResponse<Coupon> update(@RequestBody Coupon coupon) {
        couponMapper.updateById(coupon);
        return ApiResponse.success("优惠券已更新", coupon);
    }

    /** 管理端: 删除优惠券 */
    @DeleteMapping("/api/admin/coupons/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        couponMapper.deleteById(id);
        return ApiResponse.success("优惠券已删除");
    }

    /** 用户端: 查看可领取的优惠券列表 */
    @GetMapping("/api/user/coupons/available")
    public ApiResponse<List<Coupon>> listAvailable() {
        return ApiResponse.success(couponService.listAvailable());
    }

    /** 用户端: 可用优惠券列表（含优惠券详情） */
    @GetMapping("/api/user/coupons")
    public ApiResponse<List<Map<String, Object>>> listMy() {
        Long userId = getCurrentUserId();
        List<UserCoupon> userCoupons = couponService.listUserCoupons(userId);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (UserCoupon uc : userCoupons) {
            Coupon coupon = couponMapper.selectById(uc.getCouponId());
            if (coupon == null) continue;
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", uc.getId());
            item.put("couponId", coupon.getId());
            item.put("name", coupon.getName());
            item.put("type", coupon.getType());
            item.put("value", coupon.getValue());
            item.put("minOrderAmount", coupon.getMinOrderAmount());
            item.put("status", uc.getStatus());
            item.put("expireTime", uc.getExpireTime());
            item.put("obtainTime", uc.getObtainTime());
            result.add(item);
        }
        return ApiResponse.success(result);
    }

    /** 领取优惠券 */
    @PostMapping("/api/user/coupons/{couponId}/obtain")
    public ApiResponse<UserCoupon> obtain(@PathVariable Long couponId) {
        Long userId = getCurrentUserId();
        return ApiResponse.success("领取成功", couponService.obtain(userId, couponId));
    }

    /** 计算优惠金额 */
    @PostMapping("/api/user/coupons/calculate")
    public ApiResponse<Map<String, Object>> calculate(@RequestBody Map<String, Object> params) {
        Long userCouponId = Long.valueOf(params.get("userCouponId").toString());
        BigDecimal orderAmount = new BigDecimal(params.get("orderAmount").toString());
        BigDecimal discount = couponService.calculateDiscount(userCouponId, orderAmount);
        return ApiResponse.success(Map.of("discount", discount, "finalAmount", orderAmount.subtract(discount)));
    }

    /** 核销优惠券（支付时使用） */
    @PostMapping("/api/user/coupons/{userCouponId}/use")
    public ApiResponse<String> useCoupon(@PathVariable Long userCouponId,
                                          @RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());
        couponService.useCoupon(userCouponId, orderId);
        return ApiResponse.success("优惠券已使用");
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
