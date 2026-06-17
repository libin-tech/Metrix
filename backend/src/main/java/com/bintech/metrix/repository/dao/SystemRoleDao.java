package com.bintech.metrix.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.repository.entity.SystemRole;

import java.util.Collection;
import java.util.List;

public interface SystemRoleDao {
    int insert(SystemRole entity);
    int updateById(SystemRole entity);
    int deleteById(Long id);
    SystemRole selectById(Long id);
    SystemRole selectByRoleCode(String roleCode);
    List<SystemRole> selectBatchIds(Collection<Long> ids);
    List<SystemRole> selectAllActiveOrderBySortOrder();
    long countByRoleCode(String roleCode);
    IPage<SystemRole> selectRolePage(Page<SystemRole> page, String keyword);
}
