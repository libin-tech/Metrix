package com.bintech.metrix.service;

import com.bintech.metrix.dto.request.UserLoginRequest;
import com.bintech.metrix.dto.response.UserLoginResponse;
import com.bintech.metrix.repository.entity.User;

public interface UserService {
    UserLoginResponse login(UserLoginRequest request);
    void logout();
    User getCurrentUser();
    User getUserById(Long id);
    User createUser(User user);
    User updateUser(Long id, User user);
    void deleteUser(Long id);
}
