package com.ttms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ttms.entity.*;
import com.ttms.exception.BusinessException;
import com.ttms.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * B15: 会员服务
 * 管理会员等级、积分累积、余额充值、等级自动升级
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl {

    private final UserMapper userMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    /** 每消费1元积1分 */
    private static final BigDecimal POINTS_PER_YUAN = BigDecimal.ONE;

    /**
     * 获取所有会员等级
     */
    public List<MemberLevel> listLevels() {
        LambdaQueryWrapper<MemberLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(MemberLevel::getSortOrder);
        return memberLevelMapper.selectList(wrapper);
    }

    /**
     * 消费后累积积分
     */
    @Transactional
    public void accumulatePoints(Long userId, BigDecimal amount) {
        User user = userMapper.selectById(userId);
        if (user == null) return;
        int points = amount.multiply(POINTS_PER_YUAN).intValue();
        user.setPoints((user.getPoints() != null ? user.getPoints() : 0) + points);
        userMapper.updateById(user);

        // 检查是否需要升级
        checkAndUpgrade(user);
        log.info("积分累积: userId={}, +{}分, 总积分={}", userId, points, user.getPoints());
    }

    /**
     * 检查并自动升级会员等级
     */
    private void checkAndUpgrade(User user) {
        List<MemberLevel> levels = listLevels();
        MemberLevel bestLevel = null;
        BigDecimal totalSpending = BigDecimal.valueOf(user.getPoints() != null ? user.getPoints() : 0);
        for (MemberLevel level : levels) {
            if (totalSpending.compareTo(level.getMinSpending()) >= 0) {
                if (bestLevel == null || level.getMinSpending().compareTo(bestLevel.getMinSpending()) > 0) {
                    bestLevel = level;
                }
            }
        }
        if (bestLevel != null && !bestLevel.getId().equals(user.getMemberLevelId())) {
            user.setMemberLevelId(bestLevel.getId());
            userMapper.updateById(user);
            log.info("会员自动升级: userId={}, newLevel={}", user.getId(), bestLevel.getLevelName());
        }
    }

    /**
     * 获取会员折扣率
     */
    public BigDecimal getDiscountRate(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getMemberLevelId() == null) return BigDecimal.ONE;
        MemberLevel level = memberLevelMapper.selectById(user.getMemberLevelId());
        return level != null ? level.getDiscountRate() : BigDecimal.ONE;
    }

    /**
     * 储值卡充值
     */
    @Transactional
    public void recharge(Long userId, BigDecimal amount) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        user.setBalance((user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO).add(amount));
        userMapper.updateById(user);
        log.info("储值充值: userId={}, amount={}, balance={}", userId, amount, user.getBalance());
    }

    /**
     * 获取用户完整会员信息（含等级、积分、余额、下一级进度）
     */
    public java.util.Map<String, Object> getUserMembershipInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        java.util.Map<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("userId", user.getId());
        info.put("points", user.getPoints() != null ? user.getPoints() : 0);
        info.put("balance", user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO);

        // 当前等级信息
        MemberLevel currentLevel = null;
        if (user.getMemberLevelId() != null) {
            currentLevel = memberLevelMapper.selectById(user.getMemberLevelId());
        }

        if (currentLevel != null) {
            info.put("levelId", currentLevel.getId());
            info.put("levelName", currentLevel.getLevelName());
            info.put("discountRate", currentLevel.getDiscountRate());
            info.put("pointsRate", currentLevel.getPointsRate());
        } else {
            info.put("levelId", null);
            info.put("levelName", "普通用户");
            info.put("discountRate", BigDecimal.ONE);
            info.put("pointsRate", BigDecimal.ONE);
        }

        // 计算下一级进度
        List<MemberLevel> levels = listLevels();
        MemberLevel nextLevel = null;
        for (MemberLevel level : levels) {
            if (currentLevel == null || level.getMinSpending().compareTo(currentLevel.getMinSpending()) > 0) {
                if (nextLevel == null || level.getMinSpending().compareTo(nextLevel.getMinSpending()) < 0) {
                    nextLevel = level;
                }
            }
        }

        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        if (nextLevel != null) {
            info.put("nextLevelName", nextLevel.getLevelName());
            info.put("pointsNeeded", nextLevel.getMinSpending().intValue());
            info.put("pointsToNext", Math.max(0, nextLevel.getMinSpending().intValue() - currentPoints));
            int pct = nextLevel.getMinSpending().intValue() > 0
                ? Math.min(100, (int) ((long) currentPoints * 100 / nextLevel.getMinSpending().intValue()))
                : 100;
            info.put("progressPercent", pct);
        } else {
            info.put("nextLevelName", "已是最高等级");
            info.put("pointsNeeded", 0);
            info.put("pointsToNext", 0);
            info.put("progressPercent", 100);
        }

        return info;
    }

    /**
     * 管理员查看所有用户（含会员信息）
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<java.util.Map<String, Object>>
        listMemberUsers(int page, int size) {

        var pageParam = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<User>(page, size);
        // 显式过滤已销户用户（deleted=0），避免@TableLogic不生效
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);
        var userPage = userMapper.selectPage(pageParam, wrapper);

        var result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<java.util.Map<String, Object>>(
            userPage.getCurrent(), userPage.getSize(), userPage.getTotal());

        List<MemberLevel> levels = listLevels();
        java.util.Map<Long, MemberLevel> levelMap = levels.stream()
            .collect(java.util.stream.Collectors.toMap(MemberLevel::getId, l -> l));

        result.setRecords(userPage.getRecords().stream().map(u -> {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("phone", u.getPhone());
            m.put("email", u.getEmail());
            m.put("realName", u.getRealName());
            m.put("points", u.getPoints() != null ? u.getPoints() : 0);
            m.put("balance", u.getBalance() != null ? u.getBalance() : BigDecimal.ZERO);
            m.put("memberLevelId", u.getMemberLevelId());
            m.put("status", u.getStatus() != null ? u.getStatus() : 0);
            if (u.getMemberLevelId() != null && levelMap.containsKey(u.getMemberLevelId())) {
                MemberLevel level = levelMap.get(u.getMemberLevelId());
                m.put("levelName", level.getLevelName());
                m.put("discountRate", level.getDiscountRate());
            } else {
                m.put("levelName", "普通用户");
                m.put("discountRate", BigDecimal.ONE);
            }
            m.put("createTime", u.getCreateTime());
            return m;
        }).collect(java.util.stream.Collectors.toList()));

        return result;
    }

    /**
     * 管理员手动设置用户会员等级
     */
    @Transactional
    public void setUserLevel(Long userId, Long levelId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (levelId != null) {
            MemberLevel level = memberLevelMapper.selectById(levelId);
            if (level == null) throw new BusinessException("会员等级不存在");
        }
        user.setMemberLevelId(levelId);
        userMapper.updateById(user);
        log.info("管理员设置用户会员等级: userId={}, levelId={}", userId, levelId);
    }

    /**
     * 管理员手动调整用户积分（可用于客服补偿/扣减）
     * @param delta 正数增加、负数扣减
     */
    @Transactional
    public void adjustPoints(Long userId, int delta) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        int newPoints = Math.max(0, (user.getPoints() != null ? user.getPoints() : 0) + delta);
        user.setPoints(newPoints);
        userMapper.updateById(user);
        // 积分变化后重新检查升级
        checkAndUpgrade(user);
        log.info("管理员调整积分: userId={}, delta={}, newPoints={}", userId, delta, newPoints);
    }

    /**
     * 积分兑换优惠券
     * @param userId 用户ID
     * @param pointsToRedeem 兑换积分
     * @return 兑换券信息
     */
    @Transactional
    public java.util.Map<String, Object> redeemPoints(Long userId, int pointsToRedeem) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        if (currentPoints < pointsToRedeem) {
            throw new BusinessException("积分不足，当前积分: " + currentPoints);
        }
        if (pointsToRedeem < 100) {
            throw new BusinessException("最低兑换积分为100");
        }

        // 扣减积分
        user.setPoints(currentPoints - pointsToRedeem);
        userMapper.updateById(user);

        // 换算金额: 100积分 = ¥5券
        BigDecimal couponValue = BigDecimal.valueOf(pointsToRedeem * 5.0 / 100.0)
            .setScale(0, RoundingMode.HALF_UP);

        // 创建自动生成的优惠券
        Coupon coupon = new Coupon();
        coupon.setName("积分兑换券");
        coupon.setType("FIXED");
        coupon.setValue(couponValue);
        coupon.setMinOrderAmount(BigDecimal.ZERO);
        coupon.setTotalQty(1);
        coupon.setRemainingQty(1);
        coupon.setExpireDays(30);
        couponMapper.insert(coupon);

        // 发放给用户
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(coupon.getId());
        uc.setStatus(0);
        uc.setObtainTime(LocalDateTime.now());
        uc.setExpireTime(LocalDateTime.now().plusDays(30));
        userCouponMapper.insert(uc);

        log.info("积分兑换: userId={}, points={}→{}points, 获得¥{}优惠券(couponId={})",
            userId, pointsToRedeem, user.getPoints(), couponValue, coupon.getId());

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("redeemed", pointsToRedeem);
        result.put("remainingPoints", user.getPoints());
        result.put("couponValue", couponValue);
        result.put("couponId", coupon.getId());
        return result;
    }

    /**
     * 使用储值余额支付
     */
    @Transactional
    public BigDecimal payWithBalance(Long userId, BigDecimal amount) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException("余额不足，当前余额: $" + balance);
        }
        user.setBalance(balance.subtract(amount));
        userMapper.updateById(user);
        log.info("余额支付: userId={}, amount={}, remaining={}", userId, amount, user.getBalance());
        return user.getBalance();
    }
}
