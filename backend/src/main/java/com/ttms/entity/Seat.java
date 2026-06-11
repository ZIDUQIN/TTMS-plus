package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 座位表 - 记录每个场次的每个座位状态
 */
@Data
@TableName("seat")
public class Seat {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 场次ID */
    private Long scheduleId;

    /** 座位行号(从1开始) */
    private Integer seatRow;

    /** 座位列号(从1开始) */
    private Integer seatCol;

    /** 座位编号: 如"A-05" */
    private String seatNumber;

    /** 价格调整(B9座位分区定价): 默认0，正数为加价 */
    private java.math.BigDecimal priceAdjustment;

    /** 状态: 0-空闲 1-已锁定 2-已售出 3-过道/不可用 */
    private Integer status;

    /** 锁定时间(用于超时释放) */
    private LocalDateTime lockTime;

    /** 订单ID */
    private Long orderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
