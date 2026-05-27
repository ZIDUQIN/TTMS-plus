package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单表
 */
@Data
@TableName("`order`")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号: 日期+随机码 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 场次ID */
    private Long scheduleId;

    /** 影片ID */
    private Long movieId;

    /** 影厅ID */
    private Long hallId;

    /** 座位编号(逗号分隔多个) */
    private String seatNumbers;

    /** 座位数量 */
    private Integer seatCount;

    /** 总金额 */
    private BigDecimal totalPrice;

    /** 订单状态: 0-待支付 1-待观影 2-已完成 3-已改签 4-已退票 5-已过期 */
    private Integer status;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 原订单ID(改签来源) */
    private Long originalOrderId;

    /** 改签/退票时间 */
    private LocalDateTime rescheduleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /* ===== 非数据库字段 ===== */
    @TableField(exist = false)
    private String movieName;
    @TableField(exist = false)
    private String hallName;
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private LocalDateTime startTime;
    @TableField(exist = false)
    private LocalDateTime endTime;
}
