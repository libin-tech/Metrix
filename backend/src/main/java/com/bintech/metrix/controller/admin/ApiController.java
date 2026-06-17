package com.bintech.metrix.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bintech.metrix.dto.request.ApiCreateRequest;
import com.bintech.metrix.dto.request.ApiUpdateRequest;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.repository.entity.SystemApi;
import com.bintech.metrix.service.SystemApiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/apis")
@RequiredArgsConstructor
@SaCheckLogin
public class ApiController {

    private final SystemApiService apiService;

    @GetMapping
    @SaCheckPermission("system:api:list")
    public ApiResponse<IPage<SystemApi>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(apiService.page(page, size, keyword));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:api:list")
    public ApiResponse<SystemApi> getById(@PathVariable Long id) {
        return ApiResponse.success(apiService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("system:api:create")
    public ApiResponse<SystemApi> create(@Valid @RequestBody ApiCreateRequest request) {
        SystemApi api = apiService.create(request);
        return ApiResponse.success("接口创建成功", api);
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:api:update")
    public ApiResponse<SystemApi> update(@PathVariable Long id, @Valid @RequestBody ApiUpdateRequest request) {
        SystemApi api = apiService.update(id, request);
        return ApiResponse.success("接口更新成功", api);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:api:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        apiService.delete(id);
        return ApiResponse.success("接口删除成功", null);
    }

    @GetMapping("/list-all")
    @SaCheckPermission("system:api:list")
    public ApiResponse<List<SystemApi>> listAll() {
        return ApiResponse.success(apiService.listAll());
    }

}
