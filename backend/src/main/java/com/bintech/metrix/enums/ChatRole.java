package com.bintech.metrix.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatRole {

    USER("user", "用户"),
    ASSISTANT("assistant", "AI助手"),
    SYSTEM("system", "系统");

    @EnumValue
    @JsonValue
    private final String code;
    private final String description;

}
