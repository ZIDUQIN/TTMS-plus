package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("backup_log")
public class BackupLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String backupType;
    private String filePath;
    private Long fileSize;
    private Integer status;
    private String errorMsg;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
