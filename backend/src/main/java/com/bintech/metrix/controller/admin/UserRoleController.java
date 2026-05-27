package com.bintech.metrix.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.dto.request.UserAssignRoleRequest;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.repository.entity.SystemUserRole;
import com.bintech.metrix.repository.mapper.SystemRoleMapper;
import com.bintech.metrix.repository.mapper.SystemUserRoleMapper;
import com.bintech.metrix.repository.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@SaCheckLogin
public class UserRoleController {

    private final SystemUserRoleMapper systemUserRoleMapper;
    private final SystemRoleMapper systemRoleMapper;
    private final UserMapper userMapper;

    @GetMapping("/{userId}/roles")
    @SaCheckPermission("system:user:list")
    public ApiResponse<List<Long>> getUserRoles(@PathVariable Long userId) {
        if (userMapper.selectById(userId) == null) {
            return ApiResponse.error("用户不存在");
        }
        List<Long> roleIds = systemUserRoleMapper.selectList(
                new LambdaQueryWrapper<SystemUserRole>().eq(SystemUserRole::getUserId, userId)
        ).stream().map(SystemUserRole::getRoleId).toList();
        return ApiResponse.success(roleIds);
    }

    @PostMapping("/{userId}/roles")
    @SaCheckPermission("system:user:assign-role")
    public ApiResponse<Void> assignRoles(@PathVariable Long userId, @Valid @RequestBody UserAssignRoleRequest request) {
        if (userMapper.selectById(userId) == null) {
            return ApiResponse.error("用户不存在");
        }
        systemUserRoleMapper.delete(
                new LambdaQueryWrapper<SystemUserRole>().eq(SystemUserRole::getUserId, userId));
        for (Long roleId : request.getRoleIds()) {
            if (systemRoleMapper.selectById(roleId) == null) {
                return ApiResponse.error("角色ID " + roleId + " 不存在");
            }
            SystemUserRole ur = new SystemUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            ur.setCreateTime(LocalDateTime.now());
            ur.setUpdateTime(LocalDateTime.now());
            systemUserRoleMapper.insert(ur);
        }
        return ApiResponse.success("用户角色分配成功", null);
    }

}
