package com.ttms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_coupon")
public class UserCoupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long couponId;
    private Integer status;
    private Long usedOrderId;
    private LocalDateTime obtainTime;
    private LocalDateTime useTime;
    private LocalDateTime expireTime;
}
