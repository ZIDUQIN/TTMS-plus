package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shift_record")
public class ShiftRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shiftId;
    private BigDecimal cashCollected;
    private BigDecimal wechatCollected;
    private BigDecimal alipayCollected;
    private BigDecimal systemTotal;
    private BigDecimal difference;
    private Integer ticketsSold;
    private Integer ticketsRefunded;
    private String notes;
    private LocalDateTime createTime;
}
