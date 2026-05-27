package com.bintech.metrix.config;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.enums.CommonStatus;
import com.bintech.metrix.enums.UserRole;
import com.bintech.metrix.repository.entity.SystemApi;
import com.bintech.metrix.repository.entity.SystemMenu;
import com.bintech.metrix.repository.entity.SystemMenuApi;
import com.bintech.metrix.repository.entity.SystemRole;
import com.bintech.metrix.repository.entity.SystemRoleApi;
import com.bintech.metrix.repository.entity.SystemRoleMenu;
import com.bintech.metrix.repository.entity.SystemUserRole;
import com.bintech.metrix.repository.entity.User;
import com.bintech.metrix.repository.mapper.SystemApiMapper;
import com.bintech.metrix.repository.mapper.SystemMenuApiMapper;
import com.bintech.metrix.repository.mapper.SystemMenuMapper;
import com.bintech.metrix.repository.mapper.SystemRoleApiMapper;
import com.bintech.metrix.repository.mapper.SystemRoleMapper;
import com.bintech.metrix.repository.mapper.SystemRoleMenuMapper;
import com.bintech.metrix.repository.mapper.SystemUserRoleMapper;
import com.bintech.metrix.repository.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SystemUserRoleMapper systemUserRoleMapper;
    private final SystemRoleMapper systemRoleMapper;
    private final SystemRoleMenuMapper systemRoleMenuMapper;
    private final SystemRoleApiMapper systemRoleApiMapper;
    private final SystemMenuMapper systemMenuMapper;
    private final SystemApiMapper systemApiMapper;
    private final UserMapper userMapper;

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        List<SystemUserRole> userRoles = systemUserRoleMapper.selectList(
                new LambdaQueryWrapper<SystemUserRole>().eq(SystemUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            User user = userMapper.selectById(userId);
            if (user != null && user.getRole() != null) {
                return List.of(user.getRole().getCode());
            }
            return List.of();
        }
        List<Long> roleIds = userRoles.stream().map(SystemUserRole::getRoleId).toList();
        return systemRoleMapper.selectByIds(roleIds).stream()
                .filter(r -> CommonStatus.ACTIVE == r.getStatus())
                .map(SystemRole::getRoleCode)
                .toList();
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());

        List<SystemUserRole> userRoles = systemUserRoleMapper.selectList(
                new LambdaQueryWrapper<SystemUserRole>().eq(SystemUserRole::getUserId, userId));

        List<Long> roleIds;
        if (userRoles.isEmpty()) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                return List.of();
            }
            if ("ADMIN".equals(user.getRole().getCode())) {
                return allPermissionCodes();
            }
            return List.of();
        }

        roleIds = userRoles.stream().map(SystemUserRole::getRoleId).toList();
        List<SystemRole> roles = systemRoleMapper.selectByIds(roleIds).stream()
                .filter(r -> CommonStatus.ACTIVE == r.getStatus())
                .toList();

        boolean isAdmin = roles.stream().anyMatch(r -> "ADMIN".equals(r.getRoleCode()));
        if (isAdmin) {
            return allPermissionCodes();
        }

        Set<String> permissions = new HashSet<>();
        for (SystemRole role : roles) {
            List<Long> menuIds = systemRoleMenuMapper.selectList(
                    new LambdaQueryWrapper<SystemRoleMenu>().eq(SystemRoleMenu::getRoleId, role.getId())
            ).stream().map(SystemRoleMenu::getMenuId).toList();

            if (menuIds.isEmpty()) {
                continue;
            }

            List<String> menuPerms = systemMenuMapper.selectByIds(menuIds).stream()
                    .map(SystemMenu::getPermissionCode)
                    .filter(code -> code != null && !code.isEmpty())
                    .toList();
            permissions.addAll(menuPerms);


            List<String> directApiPerms = systemRoleApiMapper.selectList(
                    new LambdaQueryWrapper<SystemRoleApi>().eq(SystemRoleApi::getRoleId, role.getId())
            ).stream().map(ra -> systemApiMapper.selectById(ra.getApiId()))
                    .filter(a -> a != null && a.getPermissionCode() != null && !a.getPermissionCode().isEmpty())
                    .map(SystemApi::getPermissionCode)
                    .toList();
            permissions.addAll(directApiPerms);
        }

        return new ArrayList<>(permissions);
    }

    private List<String> allPermissionCodes() {
        Set<String> all = new HashSet<>();
        List<SystemMenu> menus = systemMenuMapper.selectList(null);
        for (SystemMenu menu : menus) {
            if (menu.getPermissionCode() != null && !menu.getPermissionCode().isEmpty()) {
                all.add(menu.getPermissionCode());
            }
        }
        List<SystemApi> apis = systemApiMapper.selectList(null);
        for (SystemApi api : apis) {
            if (api.getPermissionCode() != null && !api.getPermissionCode().isEmpty()) {
                all.add(api.getPermissionCode());
            }
        }
        return new ArrayList<>(all);
    }

}
