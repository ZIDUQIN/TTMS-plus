package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 卖品表 - 小吃、饮料、周边等
 */
@Data
@TableName("snack")
public class Snack {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 名称 */
    private String name;

    /** 分类: POPCORN/DRINK/SNACK/COMBO/OTHER */
    private String category;

    /** 单价 */
    private BigDecimal price;

    /** 图片URL */
    private String imageUrl;

    /** 描述 */
    private String description;

    /** 库存数量（-1表示无限） */
    private Integer stock;

    /** 状态: 0-下架 1-上架 */
    private Integer status;

    /** 排序 */
    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
