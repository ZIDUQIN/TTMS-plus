package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("member_level")
public class MemberLevel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String levelName;
    private BigDecimal minSpending;
    private BigDecimal discountRate;
    private BigDecimal pointsRate;
    private Integer sortOrder;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
