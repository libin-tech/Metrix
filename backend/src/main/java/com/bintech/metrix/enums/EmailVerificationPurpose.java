package com.bintech.metrix.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 邮箱验证码的使用场景。
 */
@Getter
@AllArgsConstructor
public enum EmailVerificationPurpose {

    REGISTER("REGISTER", "注册"),
    RESET_PASSWORD("RESET_PASSWORD", "重置密码");

    private final String code;
    private final String description;
}
