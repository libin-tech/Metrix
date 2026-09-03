package com.bintech.metrix.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员登录请求。
 */
@Data
public class AdminLoginRequest {

    @NotBlank(message = "管理员账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String captchaId;

    @Size(max = 4, message = "图形验证码长度不能超过4位")
    private String captchaCode;
}
