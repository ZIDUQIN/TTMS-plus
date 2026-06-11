package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("invoice")
public class Invoice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long userId;
    private String invoiceType;
    private String titleType;
    private String title;
    private String taxpayerId;
    private BigDecimal amount;
    private String invoiceNo;
    private Integer status;
    private LocalDateTime issueTime;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
