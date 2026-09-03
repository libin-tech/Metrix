package com.bintech.metrix.service;

import com.bintech.metrix.dto.request.AdminLoginRequest;
import com.bintech.metrix.dto.request.PasswordResetRequest;
import com.bintech.metrix.dto.request.UserEmailLoginRequest;
import com.bintech.metrix.dto.request.UserRegistrationRequest;
import com.bintech.metrix.dto.response.UserLoginResponse;
import com.bintech.metrix.repository.entity.User;

public interface UserService {
    UserLoginResponse loginAdmin(AdminLoginRequest request);
    UserLoginResponse loginByEmail(UserEmailLoginRequest request);
    void register(UserRegistrationRequest request);
    void resetPassword(PasswordResetRequest request);
    boolean isEmailRegistered(String email);
    void logout();
    User getCurrentUser();
    User getUserById(Long id);
    User createUser(User user);
    User updateUser(Long id, User user);
    void deleteUser(Long id);
}
