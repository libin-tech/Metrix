package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.dto.request.UserLoginRequest;
import com.bintech.metrix.dto.response.UserLoginResponse;
import com.bintech.metrix.repository.entity.User;
import com.bintech.metrix.repository.mapper.UserMapper;
import com.bintech.metrix.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @PostConstruct
    public void init() {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, BusinessConstants.DEFAULT_ADMIN_USERNAME);
        if (userMapper.selectCount(queryWrapper) == 0) {
            User admin = new User();
            admin.setUsername(BusinessConstants.DEFAULT_ADMIN_USERNAME);
            admin.setPassword(DigestUtil.md5Hex(BusinessConstants.DEFAULT_ADMIN_PASSWORD));
            admin.setEmail(BusinessConstants.DEFAULT_ADMIN_EMAIL);
            admin.setRole(BusinessConstants.DEFAULT_ADMIN_ROLE);
            admin.setIsActive(true);
            admin.setCreateTime(LocalDateTime.now());
            admin.setUpdateTime(LocalDateTime.now());
            userMapper.insert(admin);
            log.info("Default admin user created");
        }
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(queryWrapper);
        
        if (user == null) {
            throw new RuntimeException("Invalid username or password");
        }
        
        if (!DigestUtil.md5Hex(request.getPassword()).equals(user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        
        if (!user.getIsActive()) {
            throw new RuntimeException("User account is inactive");
        }
        
        StpUtil.login(user.getId());
        
        return new UserLoginResponse(
                StpUtil.getTokenValue(),
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public User getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user;
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user;
    }

    @Override
    @Transactional
    public User createUser(User user) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, user.getUsername());
        if (userMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("Username already exists");
        }
        user.setPassword(DigestUtil.md5Hex(user.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    @Override
    @Transactional
    public User updateUser(Long id, User user) {
        User existingUser = userMapper.selectById(id);
        if (existingUser == null) {
            throw new RuntimeException("User not found");
        }
        
        if (!existingUser.getUsername().equals(user.getUsername())) {
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, user.getUsername());
            if (userMapper.selectCount(queryWrapper) > 0) {
                throw new RuntimeException("Username already exists");
            }
        }
        
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(DigestUtil.md5Hex(user.getPassword()));
        }
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setRole(user.getRole());
        existingUser.setIsActive(user.getIsActive());
        existingUser.setUpdateTime(LocalDateTime.now());
        
        userMapper.updateById(existingUser);
        return existingUser;
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}
