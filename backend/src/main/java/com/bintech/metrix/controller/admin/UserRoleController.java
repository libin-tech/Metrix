package com.bintech.metrix.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.bintech.metrix.dto.request.UserAssignRoleRequest;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.repository.dao.SystemRoleDao;
import com.bintech.metrix.repository.dao.SystemUserRoleDao;
import com.bintech.metrix.repository.dao.UserDao;
import com.bintech.metrix.repository.entity.SystemUserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@SaCheckLogin
public class UserRoleController {

    private final SystemUserRoleDao systemUserRoleDao;
    private final SystemRoleDao systemRoleDao;
    private final UserDao userDao;

    @GetMapping("/{userId}/roles")
    @SaCheckPermission("system:user:list")
    public ApiResponse<List<Long>> getUserRoles(@PathVariable Long userId) {
        if (userDao.selectById(userId) == null) {
            return ApiResponse.error("用户不存在");
        }
        List<Long> roleIds = systemUserRoleDao.selectByUserId(userId)
                .stream().map(SystemUserRole::getRoleId).toList();
        return ApiResponse.success(roleIds);
    }

    @PostMapping("/{userId}/roles")
    @SaCheckPermission("system:user:assign-role")
    public ApiResponse<Void> assignRoles(@PathVariable Long userId, @Valid @RequestBody UserAssignRoleRequest request) {
        if (userDao.selectById(userId) == null) {
            return ApiResponse.error("用户不存在");
        }
        systemUserRoleDao.deleteByUserId(userId);
        for (Long roleId : request.getRoleIds()) {
            if (systemRoleDao.selectById(roleId) == null) {
                return ApiResponse.error("角色ID " + roleId + " 不存在");
            }
            SystemUserRole ur = new SystemUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            ur.setCreateTime(LocalDateTime.now());
            ur.setUpdateTime(LocalDateTime.now());
            systemUserRoleDao.insert(ur);
        }
        return ApiResponse.success("用户角色分配成功", null);
    }

}
