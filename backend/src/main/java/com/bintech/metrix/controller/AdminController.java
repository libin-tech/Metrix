package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.enums.UserRole;
import com.bintech.metrix.enums.UserStatus;
import com.bintech.metrix.repository.entity.SystemRole;
import com.bintech.metrix.repository.entity.SystemUserRole;
import com.bintech.metrix.repository.entity.User;
import com.bintech.metrix.repository.mapper.SystemRoleMapper;
import com.bintech.metrix.repository.mapper.SystemUserRoleMapper;
import com.bintech.metrix.repository.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SaCheckLogin
public class AdminController {

    private final UserMapper userMapper;
    private final SystemUserRoleMapper systemUserRoleMapper;
    private final SystemRoleMapper systemRoleMapper;

    @GetMapping("/users")
    @SaCheckPermission("system:user:list")
    public ApiResponse<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(User::getRole, UserRole.ADMIN);
        List<Long> adminUserIds = List.of();
        SystemRole adminRole = systemRoleMapper.selectOne(
                new LambdaQueryWrapper<SystemRole>().eq(SystemRole::getRoleCode, "ADMIN"));
        if (adminRole != null) {
            adminUserIds = systemUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SystemUserRole>().eq(SystemUserRole::getRoleId, adminRole.getId())
            ).stream().map(SystemUserRole::getUserId).toList();
        }
        if (!adminUserIds.isEmpty()) {
            wrapper.notIn(User::getId, adminUserIds);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getOpenid, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        IPage<User> result = userMapper.selectPage(pageParam, wrapper);

        Map<Long, List<String>> userRoleNames = new HashMap<>();
        for (User user : result.getRecords()) {
            List<Long> roleIds = systemUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SystemUserRole>().eq(SystemUserRole::getUserId, user.getId())
            ).stream().map(SystemUserRole::getRoleId).toList();
            List<String> names = roleIds.isEmpty() ? List.of()
                    : systemRoleMapper.selectByIds(roleIds).stream()
                            .map(SystemRole::getRoleName).toList();
            userRoleNames.put(user.getId(), names);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("userRoleNames", userRoleNames);
        return ApiResponse.success(response);
    }

    @PutMapping("/users/{id}/freeze")
    @SaCheckPermission("system:user:freeze")
    public ApiResponse<Void> freezeUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.get("freezeReason");
        User user = userMapper.selectById(id);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        user.setStatus(UserStatus.FROZEN);
        user.setFreezeReason(reason);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        StpUtil.kickout(user.getId());
        log.info("管理员冻结用户: userId={}, reason={}, operatorId={}", id, reason, StpUtil.getLoginIdAsLong());
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{id}/unfreeze")
    @SaCheckPermission("system:user:freeze")
    public ApiResponse<Void> unfreezeUser(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        user.setStatus(UserStatus.NORMAL);
        user.setFreezeReason(null);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("管理员解冻用户: userId={}, operatorId={}", id, StpUtil.getLoginIdAsLong());
        return ApiResponse.success(null);
    }

}
