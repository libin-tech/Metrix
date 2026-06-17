package com.bintech.metrix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.dto.request.RoleCreateRequest;
import com.bintech.metrix.dto.request.RoleUpdateRequest;
import com.bintech.metrix.dto.response.RoleVO;
import com.bintech.metrix.enums.CommonStatus;
import com.bintech.metrix.repository.dao.SystemMenuApiDao;
import com.bintech.metrix.repository.dao.SystemRoleApiDao;
import com.bintech.metrix.repository.dao.SystemRoleDao;
import com.bintech.metrix.repository.dao.SystemRoleMenuDao;
import com.bintech.metrix.repository.entity.SystemMenuApi;
import com.bintech.metrix.repository.entity.SystemRole;
import com.bintech.metrix.repository.entity.SystemRoleApi;
import com.bintech.metrix.repository.entity.SystemRoleMenu;
import com.bintech.metrix.service.SystemRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemRoleServiceImpl implements SystemRoleService {

    private final SystemRoleDao roleDao;
    private final SystemRoleMenuDao roleMenuDao;
    private final SystemRoleApiDao roleApiDao;
    private final SystemMenuApiDao menuApiDao;

    @Override
    public IPage<SystemRole> page(Integer page, Integer size, String keyword) {
        Page<SystemRole> pageParam = new Page<>(page, size);
        return roleDao.selectRolePage(pageParam, keyword);
    }

    @Override
    public SystemRole getById(Long id) {
        SystemRole role = roleDao.selectById(id);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        return role;
    }

    @Override
    public RoleVO getRoleDetail(Long id) {
        SystemRole role = getById(id);
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setIsSystem(role.getIsSystem());
        vo.setStatus(role.getStatus());
        vo.setSortOrder(role.getSortOrder());
        vo.setMenuIds(getAssignedMenuIds(id));
        vo.setApiIds(getAssignedApiIds(id));
        return vo;
    }

    @Override
    @Transactional
    public SystemRole create(RoleCreateRequest request) {
        if (roleDao.countByRoleCode(request.getRoleCode()) > 0) {
            throw new RuntimeException("角色编码已存在");
        }

        SystemRole role = new SystemRole();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setIsSystem(false);
        role.setStatus(CommonStatus.ACTIVE);
        role.setSortOrder(request.getSortOrder());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleDao.insert(role);
        return role;
    }

    @Override
    @Transactional
    public SystemRole update(Long id, RoleUpdateRequest request) {
        SystemRole role = getById(id);
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setSortOrder(request.getSortOrder());
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }
        role.setUpdateTime(LocalDateTime.now());
        roleDao.updateById(role);
        return role;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SystemRole role = getById(id);
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new RuntimeException("系统内置角色不能删除");
        }
        roleMenuDao.deleteByRoleId(id);
        roleApiDao.deleteByRoleId(id);
        roleDao.deleteById(id);
    }

    @Override
    public List<SystemRole> listAll() {
        return roleDao.selectAllActiveOrderBySortOrder();
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SystemRole role = getById(roleId);

        roleMenuDao.deleteByRoleId(roleId);

        for (Long menuId : menuIds) {
            SystemRoleMenu rm = new SystemRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            rm.setCreateTime(LocalDateTime.now());
            rm.setUpdateTime(LocalDateTime.now());
            roleMenuDao.insert(rm);
        }

        syncMenuLinkedApis(roleId, menuIds);
    }

    private void syncMenuLinkedApis(Long roleId, List<Long> menuIds) {
        roleApiDao.deleteByRoleId(roleId);

        if (menuIds.isEmpty()) {
            return;
        }

        List<SystemMenuApi> menuApis = menuApiDao.selectByMenuIdIn(menuIds);
        Set<Long> linkedApiIds = menuApis.stream()
                .map(SystemMenuApi::getApiId)
                .collect(Collectors.toSet());

        for (Long apiId : linkedApiIds) {
            SystemRoleApi ra = new SystemRoleApi();
            ra.setRoleId(roleId);
            ra.setApiId(apiId);
            ra.setCreateTime(LocalDateTime.now());
            ra.setUpdateTime(LocalDateTime.now());
            roleApiDao.insert(ra);
        }
    }

    @Override
    @Transactional
    public void assignApis(Long roleId, List<Long> apiIds) {
        SystemRole role = getById(roleId);

        roleApiDao.deleteByRoleId(roleId);

        for (Long apiId : apiIds) {
            SystemRoleApi ra = new SystemRoleApi();
            ra.setRoleId(roleId);
            ra.setApiId(apiId);
            ra.setCreateTime(LocalDateTime.now());
            ra.setUpdateTime(LocalDateTime.now());
            roleApiDao.insert(ra);
        }
    }

    @Override
    public List<Long> getAssignedMenuIds(Long roleId) {
        return roleMenuDao.selectByRoleId(roleId).stream().map(SystemRoleMenu::getMenuId).toList();
    }

    @Override
    public List<Long> getAssignedApiIds(Long roleId) {
        return roleApiDao.selectByRoleId(roleId).stream().map(SystemRoleApi::getApiId).toList();
    }

}
