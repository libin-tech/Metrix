package com.bintech.metrix.repository.dao;

import com.bintech.metrix.repository.entity.SystemRoleMenu;

import java.util.List;

public interface SystemRoleMenuDao {
    int insert(SystemRoleMenu entity);
    int deleteByRoleId(Long roleId);
    List<SystemRoleMenu> selectByRoleId(Long roleId);
}
