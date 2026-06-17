package com.bintech.metrix.repository.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.repository.entity.SystemUserRole;
import com.bintech.metrix.repository.mapper.SystemUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class SystemUserRoleDaoImpl implements SystemUserRoleDao {

    private final SystemUserRoleMapper baseMapper;

    @Override
    public int insert(SystemUserRole entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int deleteByUserId(Long userId) {
        if (userId == null) {
            log.warn("deleteByUserId: userId is null");
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<SystemUserRole>()
                .eq(SystemUserRole::getUserId, userId));
    }

    @Override
    public int deleteByRoleId(Long roleId) {
        if (roleId == null) {
            log.warn("deleteByRoleId: roleId is null");
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<SystemUserRole>()
                .eq(SystemUserRole::getRoleId, roleId));
    }

    @Override
    public List<SystemUserRole> selectByUserId(Long userId) {
        if (userId == null) {
            log.warn("selectByUserId: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<SystemUserRole>()
                .eq(SystemUserRole::getUserId, userId));
    }

    @Override
    public List<SystemUserRole> selectByRoleId(Long roleId) {
        if (roleId == null) {
            log.warn("selectByRoleId: roleId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<SystemUserRole>()
                .eq(SystemUserRole::getRoleId, roleId));
    }
}
