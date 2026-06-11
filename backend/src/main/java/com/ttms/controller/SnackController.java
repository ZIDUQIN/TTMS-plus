package com.ttms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ttms.dto.ApiResponse;
import com.ttms.entity.*;
import com.ttms.mapper.*;
import com.ttms.service.impl.MemberServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 卖品管理控制器
 * 管理小吃、饮料、套餐的CRUD和销售
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/snacks")
@RequiredArgsConstructor
public class SnackController {

    private final SnackMapper snackMapper;
    private final SnackComboMapper comboMapper;
    private final SnackOrderMapper snackOrderMapper;
    private final MemberServiceImpl memberService;

    // ===== 卖品 CRUD =====

    @GetMapping
    public ApiResponse<List<Snack>> listSnacks(@RequestParam(required = false) String category) {
        LambdaQueryWrapper<Snack> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Snack::getCategory, category);
        }
        wrapper.orderByAsc(Snack::getSortOrder);
        return ApiResponse.success(snackMapper.selectList(wrapper));
    }

    @PostMapping
    public ApiResponse<Snack> addSnack(@RequestBody Snack snack) {
        snackMapper.insert(snack);
        return ApiResponse.success("卖品已添加", snack);
    }

    @PutMapping
    public ApiResponse<Snack> updateSnack(@RequestBody Snack snack) {
        snackMapper.updateById(snack);
        return ApiResponse.success("卖品已更新", snack);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteSnack(@PathVariable Long id) {
        snackMapper.deleteById(id);
        return ApiResponse.success("卖品已删除");
    }

    // ===== 套餐 CRUD =====

    @GetMapping("/combos")
    public ApiResponse<List<SnackCombo>> listCombos() {
        List<SnackCombo> combos = comboMapper.selectList(null);
        // 关联卖品名称
        for (SnackCombo combo : combos) {
            if (combo.getSnackIds() != null && !combo.getSnackIds().isEmpty()) {
                List<Long> ids = parseSnackIds(combo.getSnackIds());
                if (!ids.isEmpty()) {
                    combo.setSnacks(snackMapper.selectBatchIds(ids));
                }
            }
        }
        return ApiResponse.success(combos);
    }

    @PostMapping("/combos")
    public ApiResponse<SnackCombo> addCombo(@RequestBody SnackCombo combo) {
        comboMapper.insert(combo);
        return ApiResponse.success("套餐已创建", combo);
    }

    @PutMapping("/combos")
    public ApiResponse<SnackCombo> updateCombo(@RequestBody SnackCombo combo) {
        comboMapper.updateById(combo);
        return ApiResponse.success("套餐已更新", combo);
    }

    @DeleteMapping("/combos/{id}")
    public ApiResponse<String> deleteCombo(@PathVariable Long id) {
        comboMapper.deleteById(id);
        return ApiResponse.success("套餐已删除");
    }

    // ===== 卖品下单（POS/管理端使用）=====

    /**
     * 创建卖品订单
     * items: [{"snackId":1,"qty":2,"price":18},...]
     */
    @PostMapping("/order")
    public ApiResponse<Map<String, Object>> createOrder(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) params.get("items");
        String paymentMethod = params.getOrDefault("paymentMethod", "CASH").toString();
        Long movieOrderId = params.get("movieOrderId") != null
            ? Long.valueOf(params.get("movieOrderId").toString()) : null;

        BigDecimal total = BigDecimal.ZERO;
        List<Map<String, Object>> processedItems = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Long snackId = Long.valueOf(item.get("snackId").toString());
            int qty = Integer.parseInt(item.get("qty").toString());
            Snack snack = snackMapper.selectById(snackId);
            if (snack == null) continue;

            BigDecimal itemTotal = snack.getPrice().multiply(BigDecimal.valueOf(qty));
            total = total.add(itemTotal);

            Map<String, Object> processed = new LinkedHashMap<>();
            processed.put("snackId", snackId);
            processed.put("name", snack.getName());
            processed.put("qty", qty);
            processed.put("price", snack.getPrice());
            processed.put("subtotal", itemTotal);
            processedItems.add(processed);

            // 扣库存
            if (snack.getStock() != null && snack.getStock() > 0) {
                snack.setStock(Math.max(0, snack.getStock() - qty));
                snackMapper.updateById(snack);
            }
        }

        String orderNo = "SNK" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + String.format("%06d", new Random().nextInt(999999));

        SnackOrder order = new SnackOrder();
        order.setOrderNo(orderNo);
        order.setMovieOrderId(movieOrderId);
        order.setUserId(getCurrentUserId());
        order.setItems(toJson(processedItems));
        order.setTotalAmount(total);
        order.setPaymentMethod(paymentMethod);
        order.setStatus(1); // 直接完成
        order.setPayTime(LocalDateTime.now());
        snackOrderMapper.insert(order);

        // 卖品消费也累积积分
        try {
            memberService.accumulatePoints(getCurrentUserId(), total);
        } catch (Exception e) {
            log.error("卖品积分累积失败", e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", orderNo);
        result.put("totalAmount", total);
        result.put("items", processedItems);
        result.put("message", "卖品下单成功");
        log.info("卖品订单: orderNo={}, total={}, items={}", orderNo, total, items.size());
        return ApiResponse.success(result);
    }

    /** 查询卖品订单（最近100条） */
    @GetMapping("/orders")
    public ApiResponse<List<SnackOrder>> listOrders() {
        LambdaQueryWrapper<SnackOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SnackOrder::getCreateTime).last("LIMIT 100");
        return ApiResponse.success(snackOrderMapper.selectList(wrapper));
    }

    // ===== 辅助方法 =====

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

    private String toJson(Object obj) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
