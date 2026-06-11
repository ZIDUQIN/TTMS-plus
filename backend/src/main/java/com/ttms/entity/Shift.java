package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("shift")
public class Shift {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String employeeName;
}
