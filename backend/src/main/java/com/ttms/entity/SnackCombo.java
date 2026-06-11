package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 套餐表 - 组合卖品
 */
@Data
@TableName("snack_combo")
public class SnackCombo {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套餐名称 */
    private String name;

    /** 套餐总价 */
    private BigDecimal price;

    /** 原价合计（用于显示省了多少钱） */
    private BigDecimal originalPrice;

    /** 套餐包含的卖品ID列表（JSON数组: [1,2,3]） */
    private String snackIds;

    /** 描述 */
    private String description;

    /** 图片URL */
    private String imageUrl;

    /** 状态: 0-下架 1-上架 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /* ===== 非数据库字段 ===== */
    /** 关联的卖品列表 */
    @TableField(exist = false)
    private List<Snack> snacks;
}
