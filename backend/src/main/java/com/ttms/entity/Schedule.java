package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 场次表
 */
@Data
@TableName("schedule")
public class Schedule {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 影片ID */
    private Long movieId;

    /** 影厅ID */
    private Long hallId;

    /** 放映开始时间 */
    private LocalDateTime startTime;

    /** 放映结束时间 */
    private LocalDateTime endTime;

    /** 票价 */
    private BigDecimal price;

    /** 状态: 0-已取消 1-正常放映 2-已结束 */
    private Integer status;

    /** 已售座位数 */
    private Integer soldCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /* ===== 非数据库字段 ===== */
    @TableField(exist = false)
    private String movieName;
    @TableField(exist = false)
    private String hallName;
    @TableField(exist = false)
    private Integer duration;
    @TableField(exist = false)
    private String posterUrl;
    @TableField(exist = false)
    private Integer hallRowCount;
    @TableField(exist = false)
    private Integer hallColCount;
}
