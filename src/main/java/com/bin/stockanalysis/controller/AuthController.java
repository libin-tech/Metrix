package com.bin.stockanalysis.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.bin.stockanalysis.dto.request.UserLoginRequest;
import com.bin.stockanalysis.dto.response.ApiResponse;
import com.bin.stockanalysis.dto.response.UserLoginResponse;
import com.bin.stockanalysis.repository.entity.User;
import com.bin.stockanalysis.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ApiResponse<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        UserLoginResponse response = userService.login(request);
        return ApiResponse.success("Login successful", response);
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

    @GetMapping("/check")
    public ApiResponse<Boolean> checkLogin() {
        return ApiResponse.success(StpUtil.isLogin());
    }
}
