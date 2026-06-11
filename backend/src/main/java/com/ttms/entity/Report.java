package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reportType;
    private LocalDate reportDate;
    private String content;
    private LocalDateTime generatedAt;
}
