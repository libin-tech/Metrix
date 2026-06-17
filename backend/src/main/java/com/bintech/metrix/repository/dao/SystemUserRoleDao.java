package com.bintech.metrix.repository.dao;

import com.bintech.metrix.repository.entity.SystemUserRole;

import java.util.List;

public interface SystemUserRoleDao {
    int insert(SystemUserRole entity);
    int deleteByUserId(Long userId);
    int deleteByRoleId(Long roleId);
    List<SystemUserRole> selectByUserId(Long userId);
    List<SystemUserRole> selectByRoleId(Long roleId);
}
