package com.bintech.metrix.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 重置普通用户密码请求。
 */
@Data
public class PasswordResetRequest {

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.{8,20}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])(?!.*\\s).*$", message = "密码须为8-20位且包含大小写字母、数字和特殊字符，不能含空格")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    private String emailCode;
}
