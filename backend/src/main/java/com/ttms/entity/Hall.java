package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 影厅表
 */
@Data
@TableName("hall")
public class Hall {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 影厅名称: 1号厅/IMAX巨幕厅/VIP厅 */
    @JsonProperty("name")
    private String hallName;

    /** 座位行数 */
    @JsonProperty("rows")
    private Integer rowCount;

    /** 座位列数 */
    @JsonProperty("cols")
    private Integer colCount;

    /** 总容量 */
    private Integer capacity;

    /** 影厅类型: STANDARD/IMAX/VIP/4DX */
    @JsonProperty("type")
    private String hallType;

    /** 状态: 0-维护中 1-正常 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 座位布局JSON: 记录不可用座位位置,如["1-5","2-10"] */
    private String seatLayout;

    /** 增强布局配置JSON(B18): 完整座位布局含过道/情侣座/无障碍座 */
    private String layoutCfg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
