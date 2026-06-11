package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private Integer status;
    private String templateCode;
    private LocalDateTime sendTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
