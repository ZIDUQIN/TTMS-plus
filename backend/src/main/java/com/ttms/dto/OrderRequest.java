package com.ttms.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    @NotNull(message = "场次ID不能为空")
    private Long scheduleId;

    @NotEmpty(message = "至少选择一个座位")
    private List<String> seatNumbers;

    /** 每个座位对应的票种: STUDENT/CHILD/SENIOR/DISABLED/MILITARY/null=普通 */
    private List<String> ticketTypes;

    /** 支付方式: WECHAT/ALIPAY/CASH/BALANCE */
    private String paymentMethod;

    /** 是否使用会员余额支付 */
    private Boolean useBalance;

    /** 目标用户ID（管理端协助下单时使用） */
    private Long userId;
}
