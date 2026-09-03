package com.bintech.metrix.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 普通用户邮箱登录请求。
 */
@Data
public class UserEmailLoginRequest {

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String captchaId;

    @Size(max = 4, message = "图形验证码长度不能超过4位")
    private String captchaCode;
}
