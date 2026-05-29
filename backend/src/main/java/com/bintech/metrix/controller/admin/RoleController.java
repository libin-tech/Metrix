package com.bintech.metrix.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bintech.metrix.annotation.Audit;
import com.bintech.metrix.dto.request.RoleAssignApiRequest;
import com.bintech.metrix.dto.request.RoleAssignMenuRequest;
import com.bintech.metrix.dto.request.RoleCreateRequest;
import com.bintech.metrix.dto.request.RoleUpdateRequest;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.RoleVO;
import com.bintech.metrix.repository.entity.SystemRole;
import com.bintech.metrix.service.SystemRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Audit(resourceType = "系统角色")
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@SaCheckLogin
public class RoleController {

    private final SystemRoleService roleService;

    @GetMapping
    @SaCheckPermission("system:role:list")
    public ApiResponse<IPage<SystemRole>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(roleService.page(page, size, keyword));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:role:list")
    public ApiResponse<RoleVO> getById(@PathVariable Long id) {
        return ApiResponse.success(roleService.getRoleDetail(id));
    }

    @PostMapping
    @SaCheckPermission("system:role:create")
    public ApiResponse<SystemRole> create(@Valid @RequestBody RoleCreateRequest request) {
        SystemRole role = roleService.create(request);
        return ApiResponse.success("角色创建成功", role);
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:role:update")
    public ApiResponse<SystemRole> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        SystemRole role = roleService.update(id, request);
        return ApiResponse.success("角色更新成功", role);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:role:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success("角色删除成功", null);
    }

    @GetMapping("/list-all")
    @SaCheckPermission("system:role:list")
    public ApiResponse<List<SystemRole>> listAll() {
        return ApiResponse.success(roleService.listAll());
    }

    @GetMapping("/{id}/menus")
    @SaCheckPermission("system:role:list")
    public ApiResponse<List<Long>> getAssignedMenus(@PathVariable Long id) {
        return ApiResponse.success(roleService.getAssignedMenuIds(id));
    }

    @PostMapping("/{id}/menus")
    @SaCheckPermission("system:role:assign-menu")
    public ApiResponse<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody RoleAssignMenuRequest request) {
        roleService.assignMenus(id, request.getMenuIds());
        return ApiResponse.success("菜单权限分配成功", null);
    }

    @GetMapping("/{id}/apis")
    @SaCheckPermission("system:role:list")
    public ApiResponse<List<Long>> getAssignedApis(@PathVariable Long id) {
        return ApiResponse.success(roleService.getAssignedApiIds(id));
    }

    @PostMapping("/{id}/apis")
    @SaCheckPermission("system:role:assign-api")
    public ApiResponse<Void> assignApis(@PathVariable Long id, @Valid @RequestBody RoleAssignApiRequest request) {
        roleService.assignApis(id, request.getApiIds());
        return ApiResponse.success("接口权限分配成功", null);
    }

}
