package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.crypto.digest.DigestUtil;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.dto.request.UserLoginRequest;
import com.bintech.metrix.dto.response.UserLoginResponse;
import com.bintech.metrix.enums.UserRole;
import com.bintech.metrix.enums.UserStatus;
import com.bintech.metrix.repository.dao.UserDao;
import com.bintech.metrix.repository.entity.User;
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

    private final UserDao userDao;

    @PostConstruct
    public void init() {
        if (userDao.countByUsername(BusinessConstants.DEFAULT_ADMIN_USERNAME) == 0) {
            User admin = new User();
            admin.setUsername(BusinessConstants.DEFAULT_ADMIN_USERNAME);
            admin.setPassword(DigestUtil.md5Hex(BusinessConstants.DEFAULT_ADMIN_PASSWORD));
            admin.setEmail(BusinessConstants.DEFAULT_ADMIN_EMAIL);
            admin.setRole(UserRole.ADMIN);
            admin.setIsActive(true);
            admin.setCreateTime(LocalDateTime.now());
            admin.setUpdateTime(LocalDateTime.now());
            userDao.insert(admin);
            log.info("Default admin user created");
        }
    }

    /**
     * 用户名密码登录，校验密码和账号状态后颁发Sa-Token
     */
    @Override
    public UserLoginResponse login(UserLoginRequest request) {
        User user = userDao.selectByUsername(request.getUsername());
        
        if (user == null) {
            throw new RuntimeException("Invalid username or password");
        }
        
        if (!DigestUtil.md5Hex(request.getPassword()).equals(user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        
        if (user.getStatus() != UserStatus.NORMAL) {
            throw new RuntimeException("账号已被" + (user.getFreezeReason() != null ? "冻结：" + user.getFreezeReason() : "禁用"));
        }
        
        StpUtil.login(user.getId(), new SaLoginParameter().setExtra("username", user.getUsername()));
        
        return new UserLoginResponse(
                StpUtil.getTokenValue(),
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getNickname(),
                user.getAvatar()
        );
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public User getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userDao.selectById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user;
    }

    @Override
    public User getUserById(Long id) {
        User user = userDao.selectById(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user;
    }

    @Override
    @Transactional
    public User createUser(User user) {
        if (userDao.countByUsername(user.getUsername()) > 0) {
            throw new RuntimeException("Username already exists");
        }
        user.setPassword(DigestUtil.md5Hex(user.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userDao.insert(user);
        return user;
    }

    @Override
    @Transactional
    public User updateUser(Long id, User user) {
        User existingUser = userDao.selectById(id);
        if (existingUser == null) {
            throw new RuntimeException("User not found");
        }
        
        if (!existingUser.getUsername().equals(user.getUsername())) {
            if (userDao.countByUsernameAndNotId(user.getUsername(), id) > 0) {
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
        
        userDao.updateById(existingUser);
        return existingUser;
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userDao.deleteById(id);
    }
}
