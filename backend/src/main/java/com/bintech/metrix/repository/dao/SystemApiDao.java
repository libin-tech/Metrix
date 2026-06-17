package com.bintech.metrix.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.repository.entity.SystemApi;

import java.util.List;

public interface SystemApiDao {
    int insert(SystemApi entity);
    int updateById(SystemApi entity);
    int deleteById(Long id);
    SystemApi selectById(Long id);
    SystemApi selectByPathAndMethod(String path, String method);
    List<SystemApi> selectAll();
    IPage<SystemApi> selectApiPage(Page<SystemApi> page, String keyword);
    long countByPermissionCode(String permissionCode);
    long countByPermissionCodeExcludeId(String permissionCode, Long excludeId);
}
