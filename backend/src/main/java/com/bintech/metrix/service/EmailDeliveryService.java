package com.bintech.metrix.service;

import com.bintech.metrix.enums.EmailVerificationPurpose;

/**
 * 邮件投递服务。
 */
public interface EmailDeliveryService {

    boolean isAvailable();

    void sendVerificationCode(String email, EmailVerificationPurpose purpose, String code);
}
