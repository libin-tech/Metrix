package com.bintech.metrix.repository.dao;

import com.bintech.metrix.repository.entity.SystemMenu;

import java.util.Collection;
import java.util.List;

public interface SystemMenuDao {
    int insert(SystemMenu entity);
    int updateById(SystemMenu entity);
    int deleteById(Long id);
    int deleteByParentId(Long parentId);
    SystemMenu selectById(Long id);
    List<SystemMenu> selectAll();
    List<SystemMenu> selectBatchIds(Collection<Long> ids);
    List<SystemMenu> selectByParentId(Long parentId);
    long countByParentId(Long parentId);
    long countByPermissionCode(String permissionCode);
    long countByPermissionCodeExcludeId(String permissionCode, Long excludeId);
}
