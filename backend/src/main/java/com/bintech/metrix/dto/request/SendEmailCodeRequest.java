package com.bintech.metrix.dto.request;

import com.bintech.metrix.enums.EmailVerificationPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送邮箱验证码请求。
 */
@Data
public class SendEmailCodeRequest {

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotNull(message = "验证码用途不能为空")
    private EmailVerificationPurpose purpose;

    private String captchaId;

    @Size(max = 4, message = "图形验证码长度不能超过4位")
    private String captchaCode;
}
