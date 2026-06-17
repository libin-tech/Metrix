package com.bintech.metrix.repository.dao;

import com.bintech.metrix.repository.entity.SystemRoleApi;

import java.util.List;

public interface SystemRoleApiDao {
    int insert(SystemRoleApi entity);
    int deleteByRoleId(Long roleId);
    List<SystemRoleApi> selectByRoleId(Long roleId);
}
