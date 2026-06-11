package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_record")
public class PaymentRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String transactionId;
    private BigDecimal amount;
    private String method;
    private Integer status;
    private LocalDateTime callbackTime;
    private String callbackData;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
