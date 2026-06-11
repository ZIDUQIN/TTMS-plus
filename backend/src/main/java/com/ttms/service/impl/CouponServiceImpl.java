package com.ttms.service.impl;

import com.ttms.entity.Coupon;
import com.ttms.entity.UserCoupon;
import com.ttms.exception.BusinessException;
import com.ttms.mapper.CouponMapper;
import com.ttms.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * B16: 优惠券服务
 * 管理优惠券的发放、查询、核销
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    /** 获取可领取的优惠券列表 */
    public List<Coupon> listAvailable() {
        return couponMapper.selectAvailable();
    }

    /** 用户领取优惠券 */
    @Transactional
    public UserCoupon obtain(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getRemainingQty() <= 0) {
            throw new BusinessException("优惠券已发放完毕");
        }
        // 检查是否已领取过
        List<UserCoupon> existing = userCouponMapper.selectUserAvailable(userId);
        boolean alreadyObtained = existing.stream().anyMatch(uc -> uc.getCouponId().equals(couponId));
        if (alreadyObtained) {
            throw new BusinessException("您已领取过此优惠券");
        }
        couponMapper.decrementQty(couponId);
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus(0);
        uc.setObtainTime(LocalDateTime.now());
        uc.setExpireTime(LocalDateTime.now().plusDays(coupon.getExpireDays() != null ? coupon.getExpireDays() : 30));
        userCouponMapper.insert(uc);
        log.info("优惠券领取: userId={}, couponId={}, name={}", userId, couponId, coupon.getName());
        return uc;
    }

    /** 用户可用优惠券列表 */
    public List<UserCoupon> listUserCoupons(Long userId) {
        return userCouponMapper.selectUserAvailable(userId);
    }

    /** 计算优惠券折扣金额 */
    public BigDecimal calculateDiscount(Long userCouponId, BigDecimal orderAmount) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null) throw new BusinessException("优惠券不存在");
        Coupon coupon = couponMapper.selectById(uc.getCouponId());
        if (coupon == null) throw new BusinessException("优惠券已失效");
        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BusinessException("订单金额不满足优惠券使用条件（满$" + coupon.getMinOrderAmount() + "可用）");
        }
        if ("FIXED".equalsIgnoreCase(coupon.getType())) {
            return coupon.getValue();
        } else if ("PERCENT".equalsIgnoreCase(coupon.getType())) {
            // value是折扣率(如0.6=打6折=优惠60%)，折扣金额=订单金额*value
            return orderAmount.multiply(coupon.getValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /** 核销优惠券 */
    @Transactional
    public void useCoupon(Long userCouponId, Long orderId) {
        int affected = userCouponMapper.useCoupon(userCouponId, orderId);
        if (affected == 0) throw new BusinessException("优惠券核销失败");
        log.info("优惠券核销: userCouponId={}, orderId={}", userCouponId, orderId);
    }
}
