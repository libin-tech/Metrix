package com.bintech.metrix.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {

    NORMAL("NORMAL", "正常"),
    FROZEN("FROZEN", "冻结");

    @EnumValue
    @JsonValue
    private final String code;
    private final String description;

}
