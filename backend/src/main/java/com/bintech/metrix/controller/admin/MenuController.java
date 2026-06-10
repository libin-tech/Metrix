package com.bintech.metrix.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.bintech.metrix.dto.request.MenuAssignApiRequest;
import com.bintech.metrix.dto.request.MenuCreateRequest;
import com.bintech.metrix.dto.request.MenuUpdateRequest;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.MenuTreeVO;
import com.bintech.metrix.repository.entity.SystemMenu;
import com.bintech.metrix.service.SystemMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/menus")
@RequiredArgsConstructor
@SaCheckLogin
public class MenuController {

    private final SystemMenuService menuService;

    @GetMapping("/tree")
    @SaCheckPermission("system:menu:list")
    public ApiResponse<MenuTreeVO> getMenuTree() {
        return ApiResponse.success(menuService.getMenuTree());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:menu:list")
    public ApiResponse<SystemMenu> getById(@PathVariable Long id) {
        return ApiResponse.success(menuService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("system:menu:create")
    public ApiResponse<SystemMenu> create(@Valid @RequestBody MenuCreateRequest request) {
        SystemMenu menu = menuService.create(request);
        return ApiResponse.success("菜单创建成功", menu);
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:menu:update")
    public ApiResponse<SystemMenu> update(@PathVariable Long id, @Valid @RequestBody MenuUpdateRequest request) {
        SystemMenu menu = menuService.update(id, request);
        return ApiResponse.success("菜单更新成功", menu);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:menu:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ApiResponse.success("菜单删除成功", null);
    }

    @GetMapping("/{id}/apis")
    @SaCheckPermission("system:menu:list")
    public ApiResponse<List<Long>> getAssignedApis(@PathVariable Long id) {
        return ApiResponse.success(menuService.getAssignedApiIds(id));
    }

    @PostMapping("/{id}/apis")
    @SaCheckPermission("system:menu:assign-api")
    public ApiResponse<Void> assignApis(@PathVariable Long id, @Valid @RequestBody MenuAssignApiRequest request) {
        menuService.assignApis(id, request.getApiIds());
        return ApiResponse.success("接口关联成功", null);
    }

}
