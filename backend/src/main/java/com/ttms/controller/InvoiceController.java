package com.ttms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.dto.ApiResponse;
import com.ttms.entity.Invoice;
import com.ttms.exception.BusinessException;
import com.ttms.mapper.InvoiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/** B24: 发票管理 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceMapper invoiceMapper;

    /** 用户申请发票 */
    @PostMapping("/user/invoices")
    public ApiResponse<Invoice> request(@RequestBody Invoice invoice) {
        Long userId = getCurrentUserId();
        invoice.setUserId(userId);
        invoice.setStatus(0);
        invoiceMapper.insert(invoice);
        log.info("发票申请: userId={}, orderId={}, amount={}", userId, invoice.getOrderId(), invoice.getAmount());
        return ApiResponse.success("发票申请已提交");
    }

    /** 用户发票列表 */
    @GetMapping("/user/invoices")
    public ApiResponse<Page<Invoice>> listMy(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Page<Invoice> p = new Page<>(page, size);
        LambdaQueryWrapper<Invoice> w = new LambdaQueryWrapper<>();
        w.eq(Invoice::getUserId, userId).orderByDesc(Invoice::getCreateTime);
        return ApiResponse.success(invoiceMapper.selectPage(p, w));
    }

    /** 管理端: 发票列表 */
    @GetMapping("/admin/invoices")
    public ApiResponse<Page<Invoice>> adminList(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        Page<Invoice> p = new Page<>(page, size);
        LambdaQueryWrapper<Invoice> w = new LambdaQueryWrapper<>();
        w.orderByDesc(Invoice::getCreateTime);
        return ApiResponse.success(invoiceMapper.selectPage(p, w));
    }

    /** 管理端: 开具发票 */
    @PutMapping("/admin/invoices/{id}/issue")
    public ApiResponse<String> issue(@PathVariable Long id, @RequestBody Map<String, String> params) {
        Invoice invoice = invoiceMapper.selectById(id);
        if (invoice == null) throw new BusinessException("发票申请不存在");
        invoice.setStatus(1);
        invoice.setInvoiceNo(params.getOrDefault("invoiceNo", "INV" + System.currentTimeMillis()));
        invoice.setIssueTime(LocalDateTime.now());
        invoiceMapper.updateById(invoice);
        return ApiResponse.success("发票已开具");
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
