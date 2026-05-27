package com.bintech.metrix.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.dto.request.MenuCreateRequest;
import com.bintech.metrix.dto.request.MenuUpdateRequest;
import com.bintech.metrix.dto.response.MenuTreeVO;
import com.bintech.metrix.enums.CommonStatus;
import com.bintech.metrix.enums.MenuType;
import com.bintech.metrix.repository.entity.SystemMenu;
import com.bintech.metrix.repository.entity.SystemMenuApi;
import com.bintech.metrix.repository.mapper.SystemMenuMapper;
import com.bintech.metrix.repository.mapper.SystemMenuApiMapper;
import com.bintech.metrix.service.SystemMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMenuServiceImpl implements SystemMenuService {

    private final SystemMenuMapper menuMapper;
    private final SystemMenuApiMapper menuApiMapper;

    @Override
    public MenuTreeVO getMenuTree() {
        List<SystemMenu> allMenus = menuMapper.selectList(
                new LambdaQueryWrapper<SystemMenu>().orderByAsc(SystemMenu::getSortOrder));
        Map<Long, List<Long>> menuApiMap = menuApiMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(
                        SystemMenuApi::getMenuId,
                        Collectors.mapping(SystemMenuApi::getApiId, Collectors.toList())
                ));

        List<MenuTreeVO> treeList = allMenus.stream()
                .map(menu -> buildMenuTreeVO(menu, menuApiMap))
                .toList();

        MenuTreeVO root = new MenuTreeVO();
        root.setMenuName("根节点");
        root.setChildren(buildTree(treeList, null));
        return root;
    }

    private MenuTreeVO buildMenuTreeVO(SystemMenu menu, Map<Long, List<Long>> menuApiMap) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setPermissionCode(menu.getPermissionCode());
        vo.setMenuType(menu.getMenuType());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        vo.setSortOrder(menu.getSortOrder());
        vo.setVisible(menu.getVisible());
        vo.setStatus(menu.getStatus());
        vo.setApiIds(menuApiMap.getOrDefault(menu.getId(), List.of()));
        return vo;
    }

    private List<MenuTreeVO> buildTree(List<MenuTreeVO> flatList, Long parentId) {
        return flatList.stream()
                .filter(node -> parentId == null ? node.getParentId() == null : parentId.equals(node.getParentId()))
                .peek(node -> node.setChildren(buildTree(flatList, node.getId())))
                .sorted(Comparator.comparingInt(MenuTreeVO::getSortOrder))
                .toList();
    }

    @Override
    public SystemMenu getById(Long id) {
        SystemMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new RuntimeException("菜单不存在");
        }
        return menu;
    }

    @Override
    @Transactional
    public SystemMenu create(MenuCreateRequest request) {
        if (request.getPermissionCode() != null) {
            LambdaQueryWrapper<SystemMenu> existsWrapper = new LambdaQueryWrapper<SystemMenu>()
                    .eq(SystemMenu::getPermissionCode, request.getPermissionCode());
            if (menuMapper.selectCount(existsWrapper) > 0) {
                throw new RuntimeException("权限标识已存在");
            }
        }

        SystemMenu menu = new SystemMenu();
        menu.setParentId(request.getParentId());
        menu.setMenuName(request.getMenuName());
        menu.setPermissionCode(request.getPermissionCode());
        menu.setMenuType(request.getMenuType());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setIcon(request.getIcon());
        menu.setSortOrder(request.getSortOrder());
        menu.setStatus(CommonStatus.ACTIVE);
        menu.setVisible(request.getVisible() != null ? request.getVisible() : true);
        menu.setCreateTime(LocalDateTime.now());
        menu.setUpdateTime(LocalDateTime.now());
        menuMapper.insert(menu);
        return menu;
    }

    @Override
    @Transactional
    public SystemMenu update(Long id, MenuUpdateRequest request) {
        if (request.getPermissionCode() != null) {
            LambdaQueryWrapper<SystemMenu> existsWrapper = new LambdaQueryWrapper<SystemMenu>()
                    .eq(SystemMenu::getPermissionCode, request.getPermissionCode())
                    .ne(SystemMenu::getId, id);
            if (menuMapper.selectCount(existsWrapper) > 0) {
                throw new RuntimeException("权限标识已存在");
            }
        }

        SystemMenu menu = getById(id);
        menu.setParentId(request.getParentId());
        menu.setMenuName(request.getMenuName());
        menu.setPermissionCode(request.getPermissionCode());
        menu.setMenuType(request.getMenuType());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setIcon(request.getIcon());
        menu.setSortOrder(request.getSortOrder());
        menu.setVisible(request.getVisible());
        if (request.getStatus() != null) {
            menu.setStatus(request.getStatus());
        }
        menu.setUpdateTime(LocalDateTime.now());
        menuMapper.updateById(menu);
        return menu;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SystemMenu menu = getById(id);
        long childCount = menuMapper.selectCount(
                new LambdaQueryWrapper<SystemMenu>().eq(SystemMenu::getParentId, id));
        if (childCount > 0) {
            throw new RuntimeException("存在子菜单，请先删除子菜单");
        }
        menuApiMapper.delete(new LambdaQueryWrapper<SystemMenuApi>().eq(SystemMenuApi::getMenuId, id));
        menuMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void assignApis(Long menuId, List<Long> apiIds) {
        getById(menuId);
        menuApiMapper.delete(new LambdaQueryWrapper<SystemMenuApi>().eq(SystemMenuApi::getMenuId, menuId));
        for (Long apiId : apiIds) {
            SystemMenuApi ma = new SystemMenuApi();
            ma.setMenuId(menuId);
            ma.setApiId(apiId);
            ma.setCreateTime(LocalDateTime.now());
            ma.setUpdateTime(LocalDateTime.now());
            menuApiMapper.insert(ma);
        }
    }

    @Override
    public List<Long> getAssignedApiIds(Long menuId) {
        return menuApiMapper.selectList(
                new LambdaQueryWrapper<SystemMenuApi>().eq(SystemMenuApi::getMenuId, menuId)
        ).stream().map(SystemMenuApi::getApiId).toList();
    }

}
