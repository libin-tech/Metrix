package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.dto.request.AdminLoginRequest;
import com.bintech.metrix.dto.request.PasswordResetRequest;
import com.bintech.metrix.dto.request.UserEmailLoginRequest;
import com.bintech.metrix.dto.request.UserRegistrationRequest;
import com.bintech.metrix.dto.response.UserLoginResponse;
import com.bintech.metrix.enums.EmailVerificationPurpose;
import com.bintech.metrix.enums.UserRole;
import com.bintech.metrix.enums.UserStatus;
import com.bintech.metrix.repository.dao.UserDao;
import com.bintech.metrix.repository.entity.User;
import com.bintech.metrix.service.EmailVerificationService;
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
    private final EmailVerificationService emailVerificationService;

    @PostConstruct
    public void init() {
        if (userDao.countByUsername(BusinessConstants.DEFAULT_ADMIN_USERNAME) == 0) {
            User admin = new User();
            admin.setUsername(BusinessConstants.DEFAULT_ADMIN_USERNAME);
            admin.setPassword(DigestUtil.md5Hex(BusinessConstants.DEFAULT_ADMIN_PASSWORD));
            admin.setEmail(BusinessConstants.DEFAULT_ADMIN_EMAIL);
            admin.setRole(UserRole.ADMIN);
            admin.setIsActive(true);
            admin.setStatus(UserStatus.NORMAL);
            admin.setCreateTime(LocalDateTime.now());
            admin.setUpdateTime(LocalDateTime.now());
            userDao.insert(admin);
            log.info("Default admin user created");
        }
    }

    /** 管理员使用账号、密码和图形验证码登录。 */
    @Override
    public UserLoginResponse loginAdmin(AdminLoginRequest request) {
        emailVerificationService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode());
        User user = userDao.selectByUsername(request.getUsername());
        validatePasswordAndStatus(user, request.getPassword(), UserRole.ADMIN);
        return login(user);
    }

    /** 普通用户使用唯一邮箱和登录密码进入工作台。 */
    @Override
    public UserLoginResponse loginByEmail(UserEmailLoginRequest request) {
        emailVerificationService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode());
        User user = userDao.selectByEmail(normalizeEmail(request.getEmail()));
        validatePasswordAndStatus(user, request.getPassword(), UserRole.USER);
        return login(user);
    }

    /** 完成邮箱验证后注册普通用户，使用 Hutool 雪花算法生成主键，昵称仅作为展示信息。 */
    @Override
    @Transactional
    public void register(UserRegistrationRequest request) {
        if (!Boolean.TRUE.equals(request.getPrivacyAgreed())) {
            throw new RuntimeException("请先同意隐私政策");
        }
        String email = normalizeEmail(request.getEmail());
        if (userDao.countByEmail(email) > 0) {
            throw new RuntimeException("该邮箱已注册，请直接登录");
        }
        emailVerificationService.verifyEmailCode(email, EmailVerificationPurpose.REGISTER, request.getEmailCode());
        User user = new User();
        user.setId(IdUtil.getSnowflakeNextId());
        user.setUsername(email);
        user.setEmail(email);
        user.setNickname(request.getNickname());
        user.setPassword(DigestUtil.md5Hex(request.getPassword()));
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setStatus(UserStatus.NORMAL);
        user.setPrivacyAgreed(true);
        user.setPrivacyAgreedTime(LocalDateTime.now());
        userDao.insert(user);
    }

    /** 已注册用户通过一次性邮箱验证码重置登录密码。 */
    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }
        String email = normalizeEmail(request.getEmail());
        User user = userDao.selectByEmail(email);
        if (user == null || user.getRole() != UserRole.USER) {
            throw new RuntimeException("该邮箱尚未注册");
        }
        emailVerificationService.verifyEmailCode(email, EmailVerificationPurpose.RESET_PASSWORD, request.getEmailCode());
        user.setPassword(DigestUtil.md5Hex(request.getPassword()));
        userDao.updateById(user);
    }

    @Override
    public boolean isEmailRegistered(String email) {
        User user = userDao.selectByEmail(normalizeEmail(email));
        return user != null && user.getRole() == UserRole.USER;
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

    private UserLoginResponse login(User user) {
        StpUtil.login(user.getId(), new SaLoginParameter().setExtra("username", user.getUsername()));
        return new UserLoginResponse(
                StpUtil.getTokenValue(), user.getId(), user.getUsername(), user.getRole(), user.getNickname(), user.getAvatar()
        );
    }

    private void validatePasswordAndStatus(User user, String password, UserRole expectedRole) {
        if (user == null || user.getRole() != expectedRole || !DigestUtil.md5Hex(password).equals(user.getPassword())) {
            throw new RuntimeException("账号或密码错误");
        }
        if (user.getStatus() != UserStatus.NORMAL || !Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("账号已被" + (user.getFreezeReason() != null ? "冻结：" + user.getFreezeReason() : "禁用"));
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
