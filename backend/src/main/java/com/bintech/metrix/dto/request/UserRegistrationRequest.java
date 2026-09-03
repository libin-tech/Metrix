package com.bintech.metrix.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 普通用户注册请求。
 */
@Data
public class UserRegistrationRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称不能超过50个字符")
    @Pattern(regexp = "^[A-Za-z\\u4E00-\\u9FFF]+$", message = "昵称仅支持中文或大小写英文字母")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.{8,20}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])(?!.*\\s).*$", message = "密码须为8-20位且包含大小写字母、数字和特殊字符，不能含空格")
    private String password;

    @NotNull(message = "请先同意隐私政策")
    @AssertTrue(message = "请先同意隐私政策")
    private Boolean privacyAgreed;

    private String emailCode;
}
