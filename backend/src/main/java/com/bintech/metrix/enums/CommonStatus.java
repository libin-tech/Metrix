package com.bintech.metrix.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonStatus {

    ACTIVE("ACTIVE", "启用"),
    DISABLED("DISABLED", "禁用");

    @EnumValue
    @JsonValue
    private final String code;
    private final String description;

}
