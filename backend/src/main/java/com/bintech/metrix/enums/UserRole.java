package com.bintech.metrix.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRole {

    ADMIN("ADMIN", "超级管理员"),
    USER("USER", "普通用户");

    @EnumValue
    @JsonValue
    private final String code;
    private final String description;

}
