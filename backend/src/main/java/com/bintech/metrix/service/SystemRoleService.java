package com.bintech.metrix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bintech.metrix.dto.request.RoleCreateRequest;
import com.bintech.metrix.dto.request.RoleUpdateRequest;
import com.bintech.metrix.dto.response.RoleVO;
import com.bintech.metrix.repository.entity.SystemRole;

import java.util.List;

public interface SystemRoleService {

    IPage<SystemRole> page(Integer page, Integer size, String keyword);

    SystemRole getById(Long id);

    RoleVO getRoleDetail(Long id);

    SystemRole create(RoleCreateRequest request);

    SystemRole update(Long id, RoleUpdateRequest request);

    void delete(Long id);

    List<SystemRole> listAll();

    void assignMenus(Long roleId, List<Long> menuIds);

    void assignApis(Long roleId, List<Long> apiIds);

    List<Long> getAssignedMenuIds(Long roleId);

    List<Long> getAssignedApiIds(Long roleId);

}
