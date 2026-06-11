package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 卖品订单表 - 记录卖品销售
 */
@Data
@TableName("snack_order")
public class SnackOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 关联的电影订单ID（可为空，单独购买卖品） */
    private Long movieOrderId;

    /** 购买用户ID */
    private Long userId;

    /** 收银员ID（柜台销售时） */
    private Long cashierId;

    /** 购买明细JSON: [{"snackId":1,"name":"爆米花","qty":2,"price":18},...] */
    private String items;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 支付方式 */
    private String paymentMethod;

    /** 状态: 0-待支付 1-已完成 2-已退款 */
    private Integer status;

    /** 支付时间 */
    private LocalDateTime payTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
