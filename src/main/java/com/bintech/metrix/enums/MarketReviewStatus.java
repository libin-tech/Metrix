package com.bintech.metrix.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MarketReviewStatus {

    REVIEWING("REVIEWING", "复盘中"),
    COMPLETED("COMPLETED", "复盘完成"),
    FAILED("FAILED", "复盘失败");

    @EnumValue
    @JsonValue
    private final String code;
    private final String description;
}
