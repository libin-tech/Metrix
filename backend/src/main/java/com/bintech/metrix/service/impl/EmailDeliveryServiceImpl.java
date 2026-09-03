package com.bintech.metrix.service.impl;

import cn.hutool.core.util.StrUtil;
import com.bintech.metrix.config.AuthProperties;
import com.bintech.metrix.enums.EmailVerificationPurpose;
import com.bintech.metrix.service.EmailDeliveryService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 使用邮件专用执行器投递认证邮件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDeliveryServiceImpl implements EmailDeliveryService {

    private static final String BRAND_NAME = "Metrix";
    private static final String SUBJECT_TEMPLATE = BRAND_NAME + " · %s验证码";
    private static final String PLAIN_TEXT_TEMPLATE = "您正在进行 Metrix %s操作。\n\n"
            + "验证码：%s\n"
            + "有效期：5 分钟\n\n"
            + "请勿向任何人泄露此验证码。如非本人操作，请忽略此邮件。\n\n"
            + "Metrix · 为独立思考而生的 AI 投研工作台";
    private static final String HTML_TEMPLATE = """
            <!doctype html>
            <html lang="zh-CN">
            <body style="margin:0;padding:0;background:#f5f7fb;color:#24324a;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','Microsoft YaHei',sans-serif;">
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="padding:36px 16px;background:#f5f7fb;">
                <tr><td align="center">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:560px;overflow:hidden;background:#ffffff;border:1px solid #e3e9f2;border-radius:16px;box-shadow:0 12px 30px rgba(27,48,80,.08);">
                    <tr><td style="padding:26px 34px;background:#1d3356;">
                      <div style="color:#ffffff;font-size:20px;font-weight:800;letter-spacing:.3px;">Metrix</div>
                      <div style="margin-top:5px;color:#c5d4e9;font-size:11px;letter-spacing:1.4px;">RESEARCH DESK</div>
                    </td></tr>
                    <tr><td style="padding:34px;">
                      <p style="margin:0 0 12px;color:#e76b55;font-size:12px;font-weight:700;letter-spacing:1px;">SECURITY VERIFICATION</p>
                      <h1 style="margin:0;color:#1d2b42;font-size:24px;line-height:1.35;">验证你的邮箱</h1>
                      <p style="margin:16px 0 0;color:#617089;font-size:14px;line-height:1.75;">你正在进行 Metrix %s操作，请使用以下验证码完成验证。</p>
                      <div style="margin:26px 0;padding:18px 20px;border:1px solid #d9e3f0;border-radius:12px;background:#f4f7fb;text-align:center;">
                        <div style="margin-bottom:7px;color:#77859a;font-size:12px;">验证码</div>
                        <div style="color:#1f385f;font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:32px;font-weight:800;letter-spacing:8px;">%s</div>
                      </div>
                      <p style="margin:0;color:#617089;font-size:13px;line-height:1.7;">验证码将在 <strong style="color:#314665;">5 分钟</strong> 后失效。请勿向任何人泄露此验证码。</p>
                      <div style="height:1px;margin:27px 0 19px;background:#e8edf4;"></div>
                      <p style="margin:0;color:#8a96a8;font-size:12px;line-height:1.7;">Metrix 是为独立思考者打造的 AI 投研工作台，帮助你将市场数据、AI 洞察与研究过程沉淀为可执行的判断。</p>
                    </td></tr>
                    <tr><td style="padding:17px 34px;background:#fafbfd;color:#9aa5b5;font-size:11px;line-height:1.6;">此邮件由系统自动发送；如非本人操作，请直接忽略。</td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """;

    private final AuthProperties authProperties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Override
    public boolean isAvailable() {
        return mailSenderProvider.getIfAvailable() != null;
    }

    /**
     * 在独立虚拟线程中调用 SMTP，避免网络 I/O 阻塞认证请求线程。
     */
    @Override
    @Async("mailTaskExecutor")
    public void sendVerificationCode(String email, EmailVerificationPurpose purpose, String code) {
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                log.error("Email service is unavailable: purpose={}, email={}", purpose.getCode(), email);
                return;
            }
            mailSender.send(createVerificationMessage(mailSender, email, purpose, code));
            log.info("Email verification code sent: purpose={}, email={}", purpose.getCode(), email);
        } catch (MessagingException | RuntimeException exception) {
            log.error("Failed to send email verification code: purpose={}, email={}", purpose.getCode(), email, exception);
        }
    }

    private MimeMessage createVerificationMessage(JavaMailSender mailSender, String email,
                                                   EmailVerificationPurpose purpose, String code) throws MessagingException {
        MimeMessageHelper helper = new MimeMessageHelper(mailSender.createMimeMessage(), true, StandardCharsets.UTF_8.name());
        if (StrUtil.isNotBlank(authProperties.getMailFrom())) {
            helper.setFrom(authProperties.getMailFrom());
        }
        helper.setTo(email);
        helper.setSubject(SUBJECT_TEMPLATE.formatted(purpose.getDescription()));
        helper.setText(PLAIN_TEXT_TEMPLATE.formatted(purpose.getDescription(), code),
                HTML_TEMPLATE.formatted(purpose.getDescription(), code));
        return helper.getMimeMessage();
    }
}
