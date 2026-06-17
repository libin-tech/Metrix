package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.enums.UserStatus;
import com.bintech.metrix.repository.dao.SystemRoleDao;
import com.bintech.metrix.repository.dao.SystemUserRoleDao;
import com.bintech.metrix.repository.dao.UserDao;
import com.bintech.metrix.repository.entity.SystemRole;
import com.bintech.metrix.repository.entity.SystemUserRole;
import com.bintech.metrix.repository.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SaCheckLogin
public class AdminController {

    private final UserDao userDao;
    private final SystemUserRoleDao systemUserRoleDao;
    private final SystemRoleDao systemRoleDao;

    @GetMapping("/users")
    @SaCheckPermission("system:user:list")
    public ApiResponse<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        List<Long> adminUserIds = List.of();
        SystemRole adminRole = systemRoleDao.selectByRoleCode("ADMIN");
        if (adminRole != null) {
            adminUserIds = systemUserRoleDao.selectByRoleId(adminRole.getId())
                    .stream().map(SystemUserRole::getUserId).toList();
        }
        String kw = (keyword != null && !keyword.isEmpty()) ? keyword : null;
        IPage<User> result = userDao.selectUserPage(pageParam, kw, adminUserIds);

        Map<Long, List<String>> userRoleNames = new HashMap<>();
        for (User user : result.getRecords()) {
            List<Long> roleIds = systemUserRoleDao.selectByUserId(user.getId())
                    .stream().map(SystemUserRole::getRoleId).toList();
            List<String> names = roleIds.isEmpty() ? List.of()
                    : systemRoleDao.selectBatchIds(roleIds).stream()
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
        User user = userDao.selectById(id);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        user.setStatus(UserStatus.FROZEN);
        user.setFreezeReason(reason);
        user.setUpdateTime(LocalDateTime.now());
        userDao.updateById(user);

        StpUtil.kickout(user.getId());
        log.info("管理员冻结用户: userId={}, reason={}, operatorId={}", id, reason, StpUtil.getLoginIdAsLong());
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{id}/unfreeze")
    @SaCheckPermission("system:user:freeze")
    public ApiResponse<Void> unfreezeUser(@PathVariable Long id) {
        User user = userDao.selectById(id);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        user.setStatus(UserStatus.NORMAL);
        user.setFreezeReason(null);
        user.setUpdateTime(LocalDateTime.now());
        userDao.updateById(user);
        log.info("管理员解冻用户: userId={}, operatorId={}", id, StpUtil.getLoginIdAsLong());
        return ApiResponse.success(null);
    }

}
