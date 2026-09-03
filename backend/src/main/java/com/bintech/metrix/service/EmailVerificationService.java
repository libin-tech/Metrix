package com.bintech.metrix.service;

import com.bintech.metrix.dto.request.SendEmailCodeRequest;
import com.bintech.metrix.dto.response.CaptchaResponse;
import com.bintech.metrix.enums.EmailVerificationPurpose;

/**
 * 图形验证码与邮箱验证码服务。
 */
public interface EmailVerificationService {

    CaptchaResponse createCaptcha();

    void verifyCaptcha(String captchaId, String captchaCode);

    void sendEmailCode(SendEmailCodeRequest request);

    void verifyEmailCode(String email, EmailVerificationPurpose purpose, String code);

    boolean isCaptchaEnabled();
}
