package com.bintech.metrix.service.impl;

import cn.hutool.core.util.StrUtil;
import com.bintech.metrix.config.AuthProperties;
import com.bintech.metrix.constants.CacheConstants;
import com.bintech.metrix.dto.request.SendEmailCodeRequest;
import com.bintech.metrix.dto.response.CaptchaResponse;
import com.bintech.metrix.enums.EmailVerificationPurpose;
import com.bintech.metrix.service.EmailDeliveryService;
import com.bintech.metrix.service.EmailVerificationService;
import com.bintech.metrix.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 通过 Redis 保存一次性图形验证码与邮箱验证码，并发送认证邮件。
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final String CAPTCHA_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CAPTCHA_LENGTH = 4;
    private static final int EMAIL_CODE_LENGTH = 6;

    private final AuthProperties authProperties;
    private final EmailDeliveryService emailDeliveryService;
    private final RedisCacheService redisCacheService;

    /**
     * 生成一次性图形验证码，避免邮件接口被自动化请求滥用。
     */
    @Override
    public CaptchaResponse createCaptcha() {
        String captchaId = UUID.randomUUID().toString();
        String captchaCode = randomText(CAPTCHA_LENGTH);
        redisCacheService.set(captchaKey(captchaId), captchaCode,
                Duration.ofSeconds(CacheConstants.AUTH_CAPTCHA_TTL_SECONDS));
        return new CaptchaResponse(captchaId, toSvgDataUrl(captchaCode));
    }

    /**
     * 校验图形验证码并立即失效，防止重放。
     */
    @Override
    public void verifyCaptcha(String captchaId, String captchaCode) {
        if (!isCaptchaEnabled()) {
            return;
        }
        if (StrUtil.isBlank(captchaId) || StrUtil.isBlank(captchaCode)) {
            throw new RuntimeException("请输入图形验证码");
        }
        String cachedCode = redisCacheService.getAndDelete(captchaKey(captchaId));
        if (!captchaCode.equalsIgnoreCase(cachedCode)) {
            throw new RuntimeException("图形验证码错误或已过期");
        }
    }

    /**
     * 经图形验证码校验后发送注册或密码重置邮件，并限制同一邮箱发送频率。
     */
    @Override
    public void sendEmailCode(SendEmailCodeRequest request) {
        verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode());
        if (!emailDeliveryService.isAvailable()) {
            throw new RuntimeException("邮件服务未配置，请设置 MAIL_HOST、MAIL_USERNAME 和 MAIL_PASSWORD");
        }
        String email = normalizedEmail(request.getEmail());
        String cooldownKey = cooldownKey(email, request.getPurpose());
        if (!redisCacheService.setIfAbsent(cooldownKey, "1",
                Duration.ofSeconds(CacheConstants.AUTH_EMAIL_SEND_COOLDOWN_SECONDS))) {
            throw new RuntimeException("验证码已发送，请 1 分钟后再试");
        }
        String code = randomDigits(EMAIL_CODE_LENGTH);
        redisCacheService.set(emailCodeKey(email, request.getPurpose()), code,
                Duration.ofSeconds(CacheConstants.AUTH_EMAIL_CODE_TTL_SECONDS));
        emailDeliveryService.sendVerificationCode(email, request.getPurpose(), code);
    }

    /**
     * 校验场景对应的邮箱验证码，成功后立即删除。
     */
    @Override
    public void verifyEmailCode(String email, EmailVerificationPurpose purpose, String code) {
        if (StrUtil.isBlank(code)) {
            throw new RuntimeException("邮箱验证码不能为空");
        }
        String key = emailCodeKey(normalizedEmail(email), purpose);
        String cachedCode = redisCacheService.get(key);
        if (!code.equals(cachedCode)) {
            throw new RuntimeException("邮箱验证码错误或已过期");
        }
        redisCacheService.delete(key);
    }

    @Override
    public boolean isCaptchaEnabled() {
        return authProperties.isCaptchaEnabled();
    }

    private String randomText(int length) {
        StringBuilder value = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int index = 0; index < length; index++) {
            value.append(CAPTCHA_ALPHABET.charAt(random.nextInt(CAPTCHA_ALPHABET.length())));
        }
        return value.toString();
    }

    private String randomDigits(int length) {
        StringBuilder value = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int index = 0; index < length; index++) {
            value.append(random.nextInt(10));
        }
        return value.toString();
    }

    private String toSvgDataUrl(String captchaCode) {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"120\" height=\"40\" viewBox=\"0 0 120 40\">"
                + "<rect width=\"120\" height=\"40\" rx=\"6\" fill=\"#eef2f7\"/>"
                + "<path d=\"M8 10 L112 31 M15 32 L99 7\" stroke=\"#c0cad7\" stroke-width=\"1.2\"/>"
                + "<text x=\"60\" y=\"27\" text-anchor=\"middle\" font-family=\"monospace\" font-size=\"23\" font-weight=\"700\" letter-spacing=\"4\" fill=\"#22334d\">"
                + captchaCode + "</text></svg>";
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    private String captchaKey(String captchaId) {
        return CacheConstants.AUTH_CAPTCHA_KEY_PREFIX + captchaId;
    }

    private String emailCodeKey(String email, EmailVerificationPurpose purpose) {
        return CacheConstants.AUTH_EMAIL_CODE_KEY_PREFIX + purpose.getCode() + ':' + email;
    }

    private String cooldownKey(String email, EmailVerificationPurpose purpose) {
        return CacheConstants.AUTH_EMAIL_SEND_COOLDOWN_KEY_PREFIX + purpose.getCode() + ':' + email;
    }

    private String normalizedEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
