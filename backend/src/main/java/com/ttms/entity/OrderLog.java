package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志表 - 改签/退票记录
 */
@Data
@TableName("order_log")
public class OrderLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 操作类型: RESCHEDULE-改签 REFUND-退票 PAY-支付 */
    private String operationType;

    /** 操作前内容(JSON) */
    private String beforeContent;

    /** 操作后内容(JSON) */
    private String afterContent;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人类型: USER/EMPLOYEE */
    private String operatorType;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
