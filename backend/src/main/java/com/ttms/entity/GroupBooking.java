package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("group_booking")
public class GroupBooking {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String contactName;
    private String contactPhone;
    private String companyName;
    private LocalDate expectedDate;
    private Integer attendeeCount;
    private Long movieId;
    private Long hallId;
    private String specialRequirements;
    private Integer status;
    private String reviewNotes;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
