package com.bintech.metrix.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.dto.request.RoleCreateRequest;
import com.bintech.metrix.dto.request.RoleUpdateRequest;
import com.bintech.metrix.dto.response.RoleVO;
import com.bintech.metrix.enums.CommonStatus;
import com.bintech.metrix.repository.entity.SystemMenuApi;
import com.bintech.metrix.repository.entity.SystemRole;
import com.bintech.metrix.repository.entity.SystemRoleApi;
import com.bintech.metrix.repository.entity.SystemRoleMenu;
import com.bintech.metrix.repository.mapper.SystemMenuApiMapper;
import com.bintech.metrix.repository.mapper.SystemRoleMapper;
import com.bintech.metrix.repository.mapper.SystemRoleApiMapper;
import com.bintech.metrix.repository.mapper.SystemRoleMenuMapper;
import com.bintech.metrix.service.SystemRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemRoleServiceImpl implements SystemRoleService {

    private final SystemRoleMapper roleMapper;
    private final SystemRoleMenuMapper roleMenuMapper;
    private final SystemRoleApiMapper roleApiMapper;
    private final SystemMenuApiMapper menuApiMapper;

    @Override
    public IPage<SystemRole> page(Integer page, Integer size, String keyword) {
        Page<SystemRole> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SystemRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(SystemRole::getRoleCode, "ADMIN");
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SystemRole::getRoleName, keyword)
                    .or().like(SystemRole::getRoleCode, keyword));
        }
        wrapper.orderByAsc(SystemRole::getSortOrder);
        return roleMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public SystemRole getById(Long id) {
        SystemRole role = roleMapper.selectById(id);
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
        LambdaQueryWrapper<SystemRole> existsWrapper = new LambdaQueryWrapper<SystemRole>()
                .eq(SystemRole::getRoleCode, request.getRoleCode());
        if (roleMapper.selectCount(existsWrapper) > 0) {
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
        roleMapper.insert(role);
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
        roleMapper.updateById(role);
        return role;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SystemRole role = getById(id);
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new RuntimeException("系统内置角色不能删除");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SystemRoleMenu>().eq(SystemRoleMenu::getRoleId, id));
        roleApiMapper.delete(new LambdaQueryWrapper<SystemRoleApi>().eq(SystemRoleApi::getRoleId, id));
        roleMapper.deleteById(id);
    }

    @Override
    public List<SystemRole> listAll() {
        LambdaQueryWrapper<SystemRole> wrapper = new LambdaQueryWrapper<SystemRole>()
                .eq(SystemRole::getStatus, CommonStatus.ACTIVE)
                .orderByAsc(SystemRole::getSortOrder);
        return roleMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SystemRole role = getById(roleId);

        roleMenuMapper.delete(new LambdaQueryWrapper<SystemRoleMenu>().eq(SystemRoleMenu::getRoleId, roleId));

        for (Long menuId : menuIds) {
            SystemRoleMenu rm = new SystemRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            rm.setCreateTime(LocalDateTime.now());
            rm.setUpdateTime(LocalDateTime.now());
            roleMenuMapper.insert(rm);
        }

        syncMenuLinkedApis(roleId, menuIds);
    }

    private void syncMenuLinkedApis(Long roleId, List<Long> menuIds) {
        roleApiMapper.delete(new LambdaQueryWrapper<SystemRoleApi>().eq(SystemRoleApi::getRoleId, roleId));

        if (menuIds.isEmpty()) {
            return;
        }

        List<SystemMenuApi> menuApis = menuApiMapper.selectList(
                new LambdaQueryWrapper<SystemMenuApi>().in(SystemMenuApi::getMenuId, menuIds));
        Set<Long> linkedApiIds = menuApis.stream()
                .map(SystemMenuApi::getApiId)
                .collect(Collectors.toSet());

        for (Long apiId : linkedApiIds) {
            SystemRoleApi ra = new SystemRoleApi();
            ra.setRoleId(roleId);
            ra.setApiId(apiId);
            ra.setCreateTime(LocalDateTime.now());
            ra.setUpdateTime(LocalDateTime.now());
            roleApiMapper.insert(ra);
        }
    }

    @Override
    @Transactional
    public void assignApis(Long roleId, List<Long> apiIds) {
        SystemRole role = getById(roleId);

        roleApiMapper.delete(new LambdaQueryWrapper<SystemRoleApi>().eq(SystemRoleApi::getRoleId, roleId));

        for (Long apiId : apiIds) {
            SystemRoleApi ra = new SystemRoleApi();
            ra.setRoleId(roleId);
            ra.setApiId(apiId);
            ra.setCreateTime(LocalDateTime.now());
            ra.setUpdateTime(LocalDateTime.now());
            roleApiMapper.insert(ra);
        }
    }

    @Override
    public List<Long> getAssignedMenuIds(Long roleId) {
        return roleMenuMapper.selectList(
                new LambdaQueryWrapper<SystemRoleMenu>().eq(SystemRoleMenu::getRoleId, roleId)
        ).stream().map(SystemRoleMenu::getMenuId).toList();
    }

    @Override
    public List<Long> getAssignedApiIds(Long roleId) {
        return roleApiMapper.selectList(
                new LambdaQueryWrapper<SystemRoleApi>().eq(SystemRoleApi::getRoleId, roleId)
        ).stream().map(SystemRoleApi::getApiId).toList();
    }

}
