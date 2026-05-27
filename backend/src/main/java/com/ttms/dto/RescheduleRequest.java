package com.ttms.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class RescheduleRequest {
    @NotNull(message = "原订单ID不能为空")
    private Long orderId;

    @NotNull(message = "新场次ID不能为空")
    private Long newScheduleId;

    @NotEmpty(message = "至少选择一个新座位")
    private List<String> newSeatNumbers;
}
