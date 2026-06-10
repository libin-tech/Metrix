package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.dto.request.UserLoginRequest;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.UserLoginResponse;
import com.bintech.metrix.exception.FrozenUserException;
import com.bintech.metrix.repository.entity.User;
import com.bintech.metrix.service.UserService;
import com.bintech.metrix.service.WechatAuthService;
import com.bintech.metrix.service.impl.WechatAuthServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    private final WechatAuthService wechatAuthService;

    @PostMapping("/login")
    public ApiResponse<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        UserLoginResponse response = userService.login(request);
        return ApiResponse.success("Login successful", response);
    }

    @PostMapping("/login-by-code")
    public ApiResponse<UserLoginResponse> loginByCode(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isEmpty()) {
            return ApiResponse.error("验证码不能为空");
        }
        try {
            UserLoginResponse response = wechatAuthService.loginByCode(code);
            return ApiResponse.success("登录成功", response);
        } catch (FrozenUserException e) {
            return ApiResponse.error(1001, e.getMessage());
        }
    }

    @GetMapping("/verify-code")
    public ApiResponse<Map<String, Object>> verifyCode(@RequestParam String code) {
        if (code == null || code.isEmpty()) {
            return ApiResponse.error("验证码不能为空");
        }

        String openid = WechatAuthServiceImpl.LOGIN_CACHE.get(code, false);
        if (openid == null) {
            return ApiResponse.error("验证码无效或已过期，请重新获取");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);

        return ApiResponse.success(result);
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
