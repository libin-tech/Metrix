package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.dto.response.UserLoginResponse;
import com.bintech.metrix.enums.UserRole;
import com.bintech.metrix.enums.UserStatus;
import com.bintech.metrix.exception.FrozenUserException;
import com.bintech.metrix.repository.entity.SystemRole;
import com.bintech.metrix.repository.entity.SystemUserRole;
import com.bintech.metrix.repository.entity.User;
import com.bintech.metrix.repository.mapper.SystemRoleMapper;
import com.bintech.metrix.repository.mapper.SystemUserRoleMapper;
import com.bintech.metrix.repository.mapper.UserMapper;
import com.bintech.metrix.service.WechatAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatAuthServiceImpl implements WechatAuthService {

    private static final String DEFAULT_AVATAR = "";

    private final UserMapper userMapper;
    private final SystemRoleMapper systemRoleMapper;
    private final SystemUserRoleMapper systemUserRoleMapper;

    private void assignDefaultRole(Long userId) {
        SystemRole userRole = systemRoleMapper.selectOne(
                new LambdaQueryWrapper<SystemRole>().eq(SystemRole::getRoleCode, "USER"));
        if (userRole == null) return;
        SystemUserRole sur = new SystemUserRole();
        sur.setUserId(userId);
        sur.setRoleId(userRole.getId());
        sur.setCreateTime(LocalDateTime.now());
        sur.setUpdateTime(LocalDateTime.now());
        systemUserRoleMapper.insert(sur);
    }

    public static final TimedCache<String, String> LOGIN_CACHE = CacheUtil.newTimedCache(300_000);

    static {
        LOGIN_CACHE.schedulePrune(1_000);
    }

    /**
     * 微信验证码登录：校验验证码、自动注册新用户、校验冻结状态、颁发Sa-Token
     */
    @Override
    @Transactional
    public UserLoginResponse loginByCode(String code) {
        String openid = LOGIN_CACHE.get(code, false);
        if (openid == null) {
            throw new RuntimeException("验证码无效或已过期，请重新获取");
        }

        LOGIN_CACHE.remove(code);

        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getOpenid, openid);
        User user = userMapper.selectOne(query);

        if (user == null) {
            LocalDateTime now = LocalDateTime.now();
            user = new User();
            user.setUsername("wechat" + RandomUtil.randomString(10));
            user.setPassword(DigestUtil.md5Hex(openid));
            user.setOpenid(openid);
            user.setNickname("wechat" + openid.substring(Math.max(0, openid.length() - 10)));
            user.setAvatar(DEFAULT_AVATAR);
            user.setRole(UserRole.USER);
            user.setStatus(UserStatus.NORMAL);
            user.setIsActive(true);
            user.setCreateTime(now);
            user.setUpdateTime(now);
            userMapper.insert(user);
            assignDefaultRole(user.getId());
            log.info("新用户通过微信验证码登录自动注册: userId={}, openid={}, nickname={}",
                    user.getId(), openid, user.getNickname());
        }

        if (user.getStatus() != UserStatus.NORMAL) {
            String reason = user.getFreezeReason();
            throw new FrozenUserException("账号已被冻结" + (reason != null ? "：" + reason : ""));
        }

        StpUtil.login(user.getId(), new SaLoginParameter().setExtra("username", user.getUsername()));

        log.info("用户验证码登录成功: userId={}, openid={}, nickname={}",
                user.getId(), openid, user.getNickname());
        return new UserLoginResponse(
                StpUtil.getTokenValue(),
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getNickname(),
                user.getAvatar()
        );
    }
}
