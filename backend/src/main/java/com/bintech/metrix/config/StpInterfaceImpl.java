package com.bintech.metrix.config;

import cn.dev33.satoken.stp.StpInterface;
import com.bintech.metrix.enums.CommonStatus;
import com.bintech.metrix.repository.dao.*;
import com.bintech.metrix.repository.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SystemUserRoleDao systemUserRoleDao;
    private final SystemRoleDao systemRoleDao;
    private final SystemRoleMenuDao systemRoleMenuDao;
    private final SystemRoleApiDao systemRoleApiDao;
    private final SystemMenuDao systemMenuDao;
    private final SystemApiDao systemApiDao;
    private final UserDao userDao;

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        List<SystemUserRole> userRoles = systemUserRoleDao.selectByUserId(userId);
        if (userRoles.isEmpty()) {
            User user = userDao.selectById(userId);
            if (user != null && user.getRole() != null) {
                return List.of(user.getRole().getCode());
            }
            return List.of();
        }
        List<Long> roleIds = userRoles.stream().map(SystemUserRole::getRoleId).toList();
        return systemRoleDao.selectBatchIds(roleIds).stream()
                .filter(r -> CommonStatus.ACTIVE == r.getStatus())
                .map(SystemRole::getRoleCode)
                .toList();
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());

        List<SystemUserRole> userRoles = systemUserRoleDao.selectByUserId(userId);

        List<Long> roleIds;
        if (userRoles.isEmpty()) {
            User user = userDao.selectById(userId);
            if (user == null) {
                return List.of();
            }
            if ("ADMIN".equals(user.getRole().getCode())) {
                return allPermissionCodes();
            }
            return List.of();
        }

        roleIds = userRoles.stream().map(SystemUserRole::getRoleId).toList();
        List<SystemRole> roles = systemRoleDao.selectBatchIds(roleIds).stream()
                .filter(r -> CommonStatus.ACTIVE == r.getStatus())
                .toList();

        boolean isAdmin = roles.stream().anyMatch(r -> "ADMIN".equals(r.getRoleCode()));
        if (isAdmin) {
            return allPermissionCodes();
        }

        Set<String> permissions = new HashSet<>();
        for (SystemRole role : roles) {
            List<Long> menuIds = systemRoleMenuDao.selectByRoleId(role.getId())
                    .stream().map(SystemRoleMenu::getMenuId).toList();

            if (menuIds.isEmpty()) {
                continue;
            }

            List<String> menuPerms = systemMenuDao.selectBatchIds(menuIds).stream()
                    .map(SystemMenu::getPermissionCode)
                    .filter(code -> code != null && !code.isEmpty())
                    .toList();
            permissions.addAll(menuPerms);


            List<String> directApiPerms = systemRoleApiDao.selectByRoleId(role.getId())
                    .stream().map(ra -> systemApiDao.selectById(ra.getApiId()))
                    .filter(a -> a != null && a.getPermissionCode() != null && !a.getPermissionCode().isEmpty())
                    .map(SystemApi::getPermissionCode)
                    .toList();
            permissions.addAll(directApiPerms);
        }

        return new ArrayList<>(permissions);
    }

    private List<String> allPermissionCodes() {
        Set<String> all = new HashSet<>();
        List<SystemMenu> menus = systemMenuDao.selectAll();
        for (SystemMenu menu : menus) {
            if (menu.getPermissionCode() != null && !menu.getPermissionCode().isEmpty()) {
                all.add(menu.getPermissionCode());
            }
        }
        List<SystemApi> apis = systemApiDao.selectAll();
        for (SystemApi api : apis) {
            if (api.getPermissionCode() != null && !api.getPermissionCode().isEmpty()) {
                all.add(api.getPermissionCode());
            }
        }
        return new ArrayList<>(all);
    }

}
