package com.bintech.metrix.service;

import com.bintech.metrix.dto.request.MenuCreateRequest;
import com.bintech.metrix.dto.request.MenuUpdateRequest;
import com.bintech.metrix.dto.response.MenuTreeVO;
import com.bintech.metrix.repository.entity.SystemMenu;

import java.util.List;

public interface SystemMenuService {

    MenuTreeVO getMenuTree();

    SystemMenu getById(Long id);

    SystemMenu create(MenuCreateRequest request);

    SystemMenu update(Long id, MenuUpdateRequest request);

    void delete(Long id);

    void assignApis(Long menuId, List<Long> apiIds);

    List<Long> getAssignedApiIds(Long menuId);

}
