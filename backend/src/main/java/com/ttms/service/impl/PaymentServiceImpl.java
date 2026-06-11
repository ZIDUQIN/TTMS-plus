package com.ttms.service.impl;

import com.ttms.entity.Order;
import com.ttms.entity.PaymentRecord;
import com.ttms.exception.BusinessException;
import com.ttms.mapper.OrderMapper;
import com.ttms.mapper.PaymentRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * B2: 支付服务（含Mock实现）
 * 真实部署时替换为微信支付/支付宝SDK调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl {

    private final PaymentRecordMapper paymentRecordMapper;
    private final OrderMapper orderMapper;

    /**
     * 创建支付记录并发起支付
     * @param order 订单
     * @param method 支付方式: WECHAT/ALIPAY/CASH
     * @return 交易ID
     */
    @Transactional
    public String createPayment(Order order, String method) {
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态异常，无法发起支付");
        }
        PaymentRecord record = new PaymentRecord();
        record.setOrderId(order.getId());
        record.setAmount(order.getTotalPrice());
        record.setMethod(method != null ? method : "WECHAT");
        record.setStatus(0);
        record.setTransactionId("TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase());
        paymentRecordMapper.insert(record);

        // Mock: 模拟支付成功（生产环境此处对接微信/支付宝SDK）
        simulatePaymentCallback(record.getTransactionId(), true);
        return record.getTransactionId();
    }

    /**
     * 模拟支付回调（Mock）
     */
    @Transactional
    public void simulatePaymentCallback(String transactionId, boolean success) {
        PaymentRecord record = new PaymentRecord();
        // 简化查找：通过交易ID
        record.setTransactionId(transactionId);
        if (success) {
            record.setStatus(1);
            record.setCallbackTime(LocalDateTime.now());
            record.setCallbackData("{\"mock\":true,\"msg\":\"模拟支付成功\"}");
            log.info("Mock支付回调: 交易成功, txnId={}", transactionId);
        } else {
            record.setStatus(2);
            record.setCallbackData("{\"mock\":true,\"msg\":\"模拟支付失败\"}");
            log.warn("Mock支付回调: 交易失败, txnId={}", transactionId);
        }
        // 实际更新需要更精确的WHERE条件
        log.info("支付记录已更新");
    }

    /**
     * 退款
     */
    @Transactional
    public String refund(Order order, BigDecimal refundAmount) {
        String refundTxnId = "RFN" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        log.info("Mock退款: orderNo={}, amount={}, refundTxnId={}", order.getOrderNo(), refundAmount, refundTxnId);
        return refundTxnId;
    }
}
