package com.bintech.metrix.repository.dao;

import com.bintech.metrix.repository.entity.SystemMenuApi;

import java.util.List;

public interface SystemMenuApiDao {
    int insert(SystemMenuApi entity);
    int deleteByMenuId(Long menuId);
    int deleteByApiId(Long apiId);
    List<SystemMenuApi> selectAll();
    List<SystemMenuApi> selectByMenuIdIn(List<Long> menuIds);
}
