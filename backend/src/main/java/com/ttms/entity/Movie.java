package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 影片表
 */
@Data
@TableName("movie")
public class Movie {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 片名 */
    @NotBlank(message = "影片名称不能为空")
    @JsonProperty("name")
    private String movieName;

    /** 类型(逗号分隔) */
    @NotBlank(message = "影片类型不能为空")
    private String genre;

    /** 时长(分钟) */
    @NotNull(message = "影片时长不能为空")
    private Integer duration;

    /** 主演 */
    private String actors;

    /** 导演 */
    private String director;

    /** 简介 */
    private String description;

    /** 海报图片URL */
    @JsonProperty("poster")
    private String posterUrl;

    /** 上映日期 */
    private LocalDate releaseDate;

    /** 基础票价 */
    @JsonProperty("price")
    private BigDecimal basePrice;

    /** 状态: 0-下架 1-上架 2-即将上映 */
    private Integer status;

    /** 是否热门置顶 */
    private Integer isHot;

    /** 排序权重(越大越靠前) */
    private Integer sortOrder;

    /** 国家/地区 */
    private String country;

    /** 语言 */
    private String language;

    /** 评分 */
    private Double rating;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
