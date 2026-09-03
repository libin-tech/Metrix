package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.dto.request.AdminLoginRequest;
import com.bintech.metrix.dto.request.PasswordResetRequest;
import com.bintech.metrix.dto.request.SendEmailCodeRequest;
import com.bintech.metrix.dto.request.UserEmailLoginRequest;
import com.bintech.metrix.dto.request.UserRegistrationRequest;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.CaptchaResponse;
import com.bintech.metrix.dto.response.UserLoginResponse;
import com.bintech.metrix.enums.EmailVerificationPurpose;
import com.bintech.metrix.repository.entity.User;
import com.bintech.metrix.service.EmailVerificationService;
import com.bintech.metrix.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 *
 * <p>提供用户登录、登出、会话状态管理等功能，基于Sa-Token实现。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/admin/login")
    public ApiResponse<UserLoginResponse> loginAdmin(@Valid @RequestBody AdminLoginRequest request) {
        UserLoginResponse response = userService.loginAdmin(request);
        return ApiResponse.success("Login successful", response);
    }

    @PostMapping("/user/login")
    public ApiResponse<UserLoginResponse> loginUser(@Valid @RequestBody UserEmailLoginRequest request) {
        return ApiResponse.success("Login successful", userService.loginByEmail(request));
    }

    @PostMapping("/user/register")
    public ApiResponse<Void> register(@Valid @RequestBody UserRegistrationRequest request) {
        userService.register(request);
        return ApiResponse.success("注册成功，请登录", null);
    }

    @PostMapping("/user/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(request);
        return ApiResponse.success("密码重置成功，请登录", null);
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        return ApiResponse.success(emailVerificationService.createCaptcha());
    }

    @PostMapping("/email-code")
    public ApiResponse<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest request) {
        if (request.getPurpose() == EmailVerificationPurpose.REGISTER
                && userService.isEmailRegistered(request.getEmail())) {
            return ApiResponse.error("该邮箱已注册，请直接登录");
        }
        if (request.getPurpose() == EmailVerificationPurpose.RESET_PASSWORD
                && !userService.isEmailRegistered(request.getEmail())) {
            return ApiResponse.error("该邮箱尚未注册");
        }
        emailVerificationService.sendEmailCode(request);
        return ApiResponse.success("验证码已发送", null);
    }

    @GetMapping("/verification-config")
    public ApiResponse<Map<String, Boolean>> verificationConfig() {
        return ApiResponse.success(Map.of("captchaEnabled", emailVerificationService.isCaptchaEnabled()));
    }

    @PostMapping("/logout")
    @SaCheckLogin
    public ApiResponse<Void> logout() {
        userService.logout();
        return ApiResponse.success("Logout successful", null);
    }

    @GetMapping("/me")
    @SaCheckLogin
    public ApiResponse<User> getCurrentUser() {
        User user = userService.getCurrentUser();
        return ApiResponse.success(user);
    }

    @GetMapping("/permissions")
    @SaCheckLogin
    public ApiResponse<List<String>> getPermissions() {
        return ApiResponse.success(StpUtil.getPermissionList());
    }

    @GetMapping("/check")
    public ApiResponse<Boolean> checkLogin() {
        return ApiResponse.success(StpUtil.isLogin());
    }
}
