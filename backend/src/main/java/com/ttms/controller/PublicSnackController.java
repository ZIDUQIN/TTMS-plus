package com.ttms.controller;

import com.ttms.dto.ApiResponse;
import com.ttms.entity.*;
import com.ttms.mapper.*;
import com.ttms.service.impl.MemberServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 公开卖品接口 — 首页套餐展示、下单，无需登录也可浏览
 */
@Slf4j
@RestController
@RequestMapping("/api/snacks")
@RequiredArgsConstructor
public class PublicSnackController {

    private final SnackComboMapper comboMapper;
    private final SnackMapper snackMapper;
    private final SnackOrderMapper snackOrderMapper;
    private final MemberServiceImpl memberService;

    /** 公开：所有上架套餐（含关联卖品详情） */
    @GetMapping("/combos")
    public ApiResponse<List<SnackCombo>> listCombos() {
        List<SnackCombo> combos = comboMapper.selectList(null);
        for (SnackCombo combo : combos) {
            if (combo.getStatus() != null && combo.getStatus() != 1) continue;
            if (combo.getSnackIds() != null && !combo.getSnackIds().isEmpty()) {
                List<Long> ids = parseSnackIds(combo.getSnackIds());
                if (!ids.isEmpty()) {
                    combo.setSnacks(snackMapper.selectBatchIds(ids));
                }
            }
        }
        combos.removeIf(c -> c.getStatus() != null && c.getStatus() != 1);
        return ApiResponse.success(combos);
    }

    /** 用户端：快速购买套餐 */
    @PostMapping("/combo-order")
    @Transactional
    public ApiResponse<Map<String, Object>> orderCombo(@RequestBody Map<String, Object> params) {
        Long comboId = Long.valueOf(params.get("comboId").toString());
        SnackCombo combo = comboMapper.selectById(comboId);
        if (combo == null) return ApiResponse.error("套餐不存在");

        List<Long> snackIds = parseSnackIds(combo.getSnackIds());
        List<Snack> snacks = snackIds.isEmpty() ? Collections.emptyList() : snackMapper.selectBatchIds(snackIds);

        // 扣库存
        for (Snack snack : snacks) {
            if (snack.getStock() != null && snack.getStock() > 0) {
                if (snack.getStock() < 1) return ApiResponse.error(snack.getName() + " 库存不足");
                snack.setStock(snack.getStock() - 1);
                snackMapper.updateById(snack);
            }
        }

        String orderNo = "SNK" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + String.format("%06d", new Random().nextInt(999999));

        SnackOrder order = new SnackOrder();
        order.setOrderNo(orderNo);
        order.setUserId(getOptionalUserId());
        order.setItems(combo.getName() + " x1");
        order.setTotalAmount(combo.getPrice());
        order.setPaymentMethod("WECHAT");
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        snackOrderMapper.insert(order);

        // 积分
        Long uid = getOptionalUserId();
        if (uid != null && combo.getPrice() != null) {
            try { memberService.accumulatePoints(uid, combo.getPrice()); } catch (Exception ignored) {}
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", orderNo);
        result.put("comboName", combo.getName());
        result.put("totalPrice", combo.getPrice());
        result.put("message", "购买成功");
        log.info("套餐下单: comboId={}, name={}, orderNo={}", comboId, combo.getName(), orderNo);
        return ApiResponse.success(result);
    }

    private Long getOptionalUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return Long.valueOf(auth.getPrincipal().toString());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private List<Long> parseSnackIds(String jsonArray) {
        try {
            String cleaned = jsonArray.replace("[", "").replace("]", "").replace("\"", "");
            return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
