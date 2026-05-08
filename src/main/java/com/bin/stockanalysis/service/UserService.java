package com.bin.stockanalysis.service;

import com.bin.stockanalysis.dto.request.UserLoginRequest;
import com.bin.stockanalysis.dto.response.UserLoginResponse;
import com.bin.stockanalysis.repository.entity.User;

public interface UserService {
    UserLoginResponse login(UserLoginRequest request);
    void logout();
    User getCurrentUser();
    User getUserById(Long id);
    User createUser(User user);
    User updateUser(Long id, User user);
    void deleteUser(Long id);
}
